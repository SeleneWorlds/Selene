import { AbstractNetworkClient } from './AbstractNetworkClient';
import { MutableNameIdMappings } from './NameIdMappings';
import type { NetworkClient } from './NetworkClient';
import {
  decodeGamePacket,
  encodeAuthenticatePacket,
  encodeCustomPayloadPacket,
  encodeFinalizeJoinPacket,
  encodePreferencesPacket,
  encodeRequestFacingPacket,
  encodeRequestMovePacket,
  type Coordinate,
} from './GameProtocol';

export interface WebSocketNetworkClientOptions {
  url: string;
  authToken: () => Promise<string>;
  locale?: string;
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
      let opened = false;
      const socket = new WebSocket(this.options.url);
      socket.binaryType = 'arraybuffer';
      this.socket = socket;

      socket.addEventListener(
        'open',
        async () => {
          opened = true;
          try {
            await this.authenticate(socket);
            this.setStatus('connected');
            resolve();
          } catch (error) {
            this.setStatus('error');
            socket.close();
            reject(error);
          }
        },
        { once: true },
      );

      socket.addEventListener('message', (event: MessageEvent<ArrayBuffer | Blob | string>) => {
        void this.handleMessage(event);
      });

      socket.addEventListener('close', () => {
        this.socket = null;
        this.setStatus('disconnected');

        if (!opened) {
          reject(new Error(`Connection to ${this.options.url} closed before opening`));
        }
      });

      socket.addEventListener(
        'error',
        () => {
          this.setStatus('error');
          reject(new Error(`Could not connect to ${this.options.url}`));
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
    this.socket = null;
    this.setStatus('disconnected');
  }

  sendMoveRequest(coordinate: Coordinate): void {
    this.sendPacket(encodeRequestMovePacket(coordinate));
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

  private async handleMessage(event: MessageEvent<ArrayBuffer | Blob | string>): Promise<void> {
    if (typeof event.data === 'string') {
      console.warn('Ignoring text message from Selene server.');
      return;
    }

    try {
      const data = event.data instanceof Blob ? await event.data.arrayBuffer() : event.data;
      const packet = decodeGamePacket(data);

      if (packet.type === 'nameIdMappings') {
        this.nameIdMappings.applyPacket(packet.scope, packet.mappings);
      }

      if (packet.type === 'unknown') {
        console.debug(`Ignoring unsupported Selene packet ${packet.packetId}.`);
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
