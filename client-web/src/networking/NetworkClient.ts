import type { NameIdMappings } from './NameIdMappings';
import type { Coordinate, GamePacket } from './GameProtocol';

export type NetworkStatus = 'idle' | 'connecting' | 'connected' | 'disconnecting' | 'disconnected' | 'error';

export type NetworkStatusListener = (status: NetworkStatus) => void;
export type GamePacketListener = (packet: GamePacket) => void;

export interface NetworkClient {
  readonly status: NetworkStatus;
  readonly nameIdMappings: NameIdMappings;
  connect: () => Promise<void>;
  disconnect: () => void;
  sendMoveRequest: (coordinate: Coordinate) => void;
  sendFacingRequest: (angle: number) => void;
  sendCustomPayload: (payloadId: string, payload: string) => void;
  addStatusListener: (listener: NetworkStatusListener) => void;
  removeStatusListener: (listener: NetworkStatusListener) => void;
  addPacketListener: (listener: GamePacketListener) => void;
  removePacketListener: (listener: GamePacketListener) => void;
}
