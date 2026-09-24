import type { NetworkApi, ClientNetworkPayload, ClientNetworkPayloadHandler } from '@/api/NetworkApi';
import type { ClientEntities } from '@/core/ClientEntities';
import type { ClientGrid } from '@/core/ClientGrid';
import type { ClientMovementGrid } from '@/core/ClientMovementGrid';
import type { GameRenderer } from '@/core/GameRenderer';
import type { GamePacket } from '@/networking/GameProtocol';
import type { NetworkClient } from '@/networking/NetworkClient';

export class ClientPacketDispatcher {
  private readonly payloadHandlers = new Map<string, Set<ClientNetworkPayloadHandler>>();

  constructor(
    private readonly networkClient: NetworkClient,
    private readonly grid: ClientGrid,
    private readonly movementGrid: ClientMovementGrid,
    private readonly entities: ClientEntities,
    private readonly renderer: GameRenderer,
  ) {}

  readonly handlePacket = (packet: GamePacket): void => {
    if (packet.type === 'customPayload') {
      this.handleCustomPayload(packet.payloadId, packet.payload);
      return;
    }

    if (packet.type === 'setControlledEntity') {
      this.movementGrid.setControlledEntityNetworkId(packet.networkId);
      return;
    }

    if (packet.type === 'setActiveGrid') {
      this.grid.applyGrid(packet.identifier);
      return;
    }

    if (packet.type === 'moveEntity') {
      this.movementGrid.confirmMove(packet.networkId, packet.duration);
    }

    this.entities.handlePacket(packet);
    this.renderer.handlePacket(packet);
  };

  getNetworkApi(): NetworkApi {
    return {
      handlePayload: (payloadId, callback) => {
        let handlers = this.payloadHandlers.get(payloadId);
        if (!handlers) {
          handlers = new Set();
          this.payloadHandlers.set(payloadId, handlers);
        }
        handlers.add(callback);
        return () => {
          handlers.delete(callback);
          if (handlers.size === 0) {
            this.payloadHandlers.delete(payloadId);
          }
        };
      },
      onConnected: (callback) => {
        if (this.networkClient.status === 'connected') {
          callback();
          return () => undefined;
        }
        const listener = () => {
          if (this.networkClient.status !== 'connected') return;
          this.networkClient.removeStatusListener(listener);
          callback();
        };
        this.networkClient.addStatusListener(listener);
        return () => this.networkClient.removeStatusListener(listener);
      },
      sendToServer: (payloadId, payload = {}) => {
        this.networkClient.sendCustomPayload(payloadId, serializeCustomPayload(payloadId, payload));
      },
    };
  }

  private handleCustomPayload(payloadId: string, encodedPayload: string): void {
    const handlers = this.payloadHandlers.get(payloadId);
    if (!handlers) {
      return;
    }

    let payload: ClientNetworkPayload;
    try {
      payload = parseCustomPayload(payloadId, encodedPayload);
    } catch (error) {
      console.error(`Error decoding Selene network payload "${payloadId}"`, error);
      return;
    }
    for (const handler of [...handlers]) {
      try {
        handler(payload);
      } catch (error) {
        console.error(`Error in Selene network payload handler "${payloadId}"`, error);
      }
    }
  }
}

function serializeCustomPayload(payloadId: string, payload: ClientNetworkPayload): string {
  return JSON.stringify(payload, (_key, value: unknown) => {
    switch (typeof value) {
      case 'undefined':
      case 'function':
      case 'symbol':
      case 'bigint':
        throw new Error(`Selene custom payload "${payloadId}" contains an unsupported value.`);
      default:
        return value;
    }
  });
}

function parseCustomPayload(payloadId: string, encodedPayload: string): ClientNetworkPayload {
  const payload: unknown = JSON.parse(encodedPayload);

  if (typeof payload !== 'object' || payload === null || Array.isArray(payload)) {
    throw new Error(`Selene custom payload "${payloadId}" must be a JSON object.`);
  }

  return payload as ClientNetworkPayload;
}
