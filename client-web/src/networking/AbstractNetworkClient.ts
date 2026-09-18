import type { GamePacket } from './GameProtocol';
import type { GamePacketListener, NetworkStatus, NetworkStatusListener } from './NetworkClient';

export abstract class AbstractNetworkClient {
  private readonly statusListeners = new Set<NetworkStatusListener>();
  private readonly packetListeners = new Set<GamePacketListener>();
  private currentStatus: NetworkStatus = 'idle';

  get status(): NetworkStatus {
    return this.currentStatus;
  }

  addStatusListener(listener: NetworkStatusListener): void {
    this.statusListeners.add(listener);
  }

  removeStatusListener(listener: NetworkStatusListener): void {
    this.statusListeners.delete(listener);
  }

  addPacketListener(listener: GamePacketListener): void {
    this.packetListeners.add(listener);
  }

  removePacketListener(listener: GamePacketListener): void {
    this.packetListeners.delete(listener);
  }

  protected setStatus(status: NetworkStatus): void {
    if (this.currentStatus === status) {
      return;
    }

    this.currentStatus = status;
    for (const listener of this.statusListeners) {
      listener(status);
    }
  }

  protected emitPacket(packet: GamePacket): void {
    for (const listener of this.packetListeners) {
      listener(packet);
    }
  }
}
