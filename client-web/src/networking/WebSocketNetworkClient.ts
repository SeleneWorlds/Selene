import { AbstractNetworkClient } from './AbstractNetworkClient';
import { MutableNameIdMappings } from './NameIdMappings';
import type { NetworkClient } from './NetworkClient';
import {
  decodeGamePacket,
  encodeAuthenticatePacket,
  encodeCustomPayloadPacket,
  encodeFinalizeJoinPacket,
  encodeHeartbeatPacket,
  encodePreferencesPacket,
  encodeRequestFacingPacket,
  encodeRequestMovePacket,
  type Coordinate,
} from './GameProtocol';

export interface WebSocketNetworkClientOptions {
  url: string;
  authToken: () => Promise<string>;
  locale?: string;
  onDisconnected?: ((reason: string) => void) | undefined;
}

export class WebSocketNetworkClient extends AbstractNetworkClient implements NetworkClient {
  readonly nameIdMappings = new MutableNameIdMappings();

  private socket: WebSocket | null = null;

  constructor(private readonly options: WebSocketNetworkClientOptions) {
    super();
  }

  connect(): Promise<void> {
    if (this.socket && this.status === 'connected') {
      return Promise.resolve();
    }

    this.setStatus('connecting');
    this.nameIdMappings.clear();

    return new Promise((resolve, reject) => {
      let connectionWasEstablished = false;
      const socket = new WebSocket(this.options.url);
      socket.binaryType = 'arraybuffer';
      this.socket = socket;
      let readTimeout: ReturnType<typeof setTimeout> | undefined;
      const resetReadTimeout = () => {
        clearTimeout(readTimeout);
        readTimeout = setTimeout(() => {
          if (this.socket !== socket) return;
          this.socket = null;
          this.setStatus('disconnected');
          socket.close();
          if (connectionWasEstablished) {
            this.options.onDisconnected?.('Connection to the server timed out.');
          } else {
            reject(new Error('Connection to the server timed out.'));
          }
        }, 30_000);
      };
      resetReadTimeout();

      socket.addEventListener(
        'open',
        async () => {
          resetReadTimeout();
          try {
            await this.authenticate(socket);
            if (this.socket !== socket) return;
            connectionWasEstablished = true;
            this.setStatus('connected');
            resolve();
          } catch (error) {
            if (this.socket !== socket) return;
            this.setStatus('error');
            socket.close();
            reject(error);
          }
        },
        { once: true },
      );

      socket.addEventListener('message', (event: MessageEvent<ArrayBuffer | Blob | string>) => {
        if (this.socket !== socket) return;
        resetReadTimeout();
        void this.handleMessage(event, socket);
      });

      socket.addEventListener('close', (event) => {
        clearTimeout(readTimeout);
        if (this.socket !== socket) return;
        const connectionWasLost = connectionWasEstablished && this.status !== 'disconnecting';
        this.socket = null;
        this.setStatus('disconnected');

        if (!connectionWasEstablished) {
          reject(new Error(`Connection to ${this.options.url} closed before authentication completed`));
        } else if (connectionWasLost) {
          this.options.onDisconnected?.(event.reason || 'Connection to the server was lost.');
        }
      });

      socket.addEventListener(
        'error',
        () => {
          if (this.socket !== socket) return;
          if (!connectionWasEstablished) {
            this.setStatus('error');
            reject(new Error(`Could not connect to ${this.options.url}`));
          }
        },
        { once: true },
      );
    });
  }

  disconnect(): void {
    if (!this.socket) {
      this.setStatus('disconnected');
      return;
    }

    this.setStatus('disconnecting');
    this.socket.close();
  }

  sendMoveRequest(coordinate: Coordinate): void {
    this.sendPacket(encodeRequestMovePacket(coordinate));
  }

  setLocale(locale: string): void {
    this.options.locale = locale;
    if (this.status === 'connected') {
      this.sendPacket(encodePreferencesPacket(locale));
    }
  }

  sendFacingRequest(angle: number): void {
    this.sendPacket(encodeRequestFacingPacket(angle));
  }

  sendCustomPayload(payloadId: string, payload: string): void {
    this.sendPacket(encodeCustomPayloadPacket(payloadId, payload));
  }

  private sendPacket(packet: ArrayBuffer): void {
    if (!this.socket || this.status !== 'connected') {
      throw new Error('Cannot send Selene packet while disconnected.');
    }

    this.socket.send(packet);
  }

  private async authenticate(socket: WebSocket): Promise<void> {
    const token = await this.options.authToken();
    socket.send(encodeAuthenticatePacket(token));
    socket.send(encodePreferencesPacket(this.options.locale ?? getDefaultLocale()));
    socket.send(encodeFinalizeJoinPacket());
  }

  private async handleMessage(event: MessageEvent<ArrayBuffer | Blob | string>, socket: WebSocket): Promise<void> {
    if (typeof event.data === 'string') {
      console.warn('Ignoring text message from Selene server.');
      return;
    }

    try {
      const data = event.data instanceof Blob ? await event.data.arrayBuffer() : event.data;
      if (this.socket !== socket) return;
      const packet = decodeGamePacket(data);
      if (packet.type === 'heartbeat') {
        socket.send(encodeHeartbeatPacket());
        return;
      }

      if (packet.type === 'nameIdMappings') {
        this.nameIdMappings.applyPacket(packet.scope, packet.mappings);
      }

      if (packet.type === 'unknown') {
        console.debug(`Ignoring unsupported Selene packet ${packet.packetId}.`);
      }

      if (packet.type === 'disconnect') {
        this.options.onDisconnected?.(packet.reason);
        this.disconnect();
        return;
      }

      this.emitPacket(packet);
    } catch (error) {
      console.warn('Failed to decode Selene server packet.', error);
    }
  }
}

function getDefaultLocale(): string {
  return navigator.language.replace(/-/g, '_');
}
