import { AbstractNetworkClient } from './AbstractNetworkClient';
import { MutableNameIdMappings } from './NameIdMappings';
import type { NetworkClient } from './NetworkClient';
import type { Coordinate } from './GameProtocol';

export class NullNetworkClient extends AbstractNetworkClient implements NetworkClient {
  readonly nameIdMappings = new MutableNameIdMappings();

  async connect(): Promise<void> {
    this.setStatus('disconnected');
  }

  disconnect(): void {
    this.setStatus('disconnected');
  }

  sendMoveRequest(_coordinate: Coordinate): void {
    throw new Error('Cannot send Selene move request without a network client.');
  }

  sendFacingRequest(_angle: number): void {
    throw new Error('Cannot send Selene facing request without a network client.');
  }

  sendCustomPayload(_payloadId: string, _payload: string): void {
    throw new Error('Cannot send Selene custom payload without a network client.');
  }
}
