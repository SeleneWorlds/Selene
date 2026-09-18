import type { ClientDirection } from '@/api/GridApi';
import type { EntitiesApi } from '@/api/EntitiesApi';
import type { MovementGridApi } from '@/api/MovementGridApi';
import type { NetworkClient } from '@/networking/NetworkClient';

export class ClientMovementGrid implements MovementGridApi {
  private controlledEntityNetworkId = -1;
  private moveDirection: ClientDirection | null = null;
  private facingDirection: ClientDirection | null = null;
  private requestedStep = false;
  private movementRemainingMs = 0;

  constructor(
    private readonly entities: EntitiesApi,
    private readonly networkClient: NetworkClient,
  ) {}

  setControlledEntityNetworkId(networkId: number): void {
    this.controlledEntityNetworkId = networkId;
    this.requestedStep = false;
    this.movementRemainingMs = 0;
  }

  setMotion(direction: ClientDirection): void {
    this.moveDirection = direction;
  }

  setFacing(direction: ClientDirection): void {
    this.facingDirection = direction;
  }

  update(deltaMs: number): void {
    this.movementRemainingMs = Math.max(0, this.movementRemainingMs - deltaMs);
    const entity = this.entities.getEntityByNetworkId(this.controlledEntityNetworkId);

    if (entity && this.moveDirection && !this.requestedStep && this.movementRemainingMs === 0) {
      const coordinate = entity.getCoordinate();
      this.networkClient.sendMoveRequest({
        x: coordinate.x + this.moveDirection.vector.x,
        y: coordinate.y + this.moveDirection.vector.y,
        z: coordinate.z + this.moveDirection.vector.z,
      });
      this.requestedStep = true;
    }

    if (entity && this.facingDirection && entity.getFacing() !== this.facingDirection.angle) {
      this.networkClient.sendFacingRequest(this.facingDirection.angle);
    }

    this.moveDirection = null;
    this.facingDirection = null;
  }

  confirmMove(networkId: number, durationSeconds: number): void {
    if (this.controlledEntityNetworkId === networkId) {
      this.requestedStep = false;
      this.movementRemainingMs = Math.max(0, durationSeconds * 1000);
    }
  }
}
