import { projectCoordinate } from '@/core/WorldProjection';
import type { Coordinate } from '@/networking/GameProtocol';

export type CameraCoordinateChangedListener = (coordinate: Coordinate) => void;

export class ClientCamera {
  private readonly coordinateChangedListeners = new Set<CameraCoordinateChangedListener>();
  private viewportWidth = 1;
  private viewportHeight = 1;
  private viewportX = 0;
  private viewportY = 0;
  private hasCustomViewport = false;
  private coordinate: Coordinate = { x: 0, y: 0, z: 0 };
  private followedEntityId: number | null = null;

  setViewport(width: number, height: number): void {
    if (this.hasCustomViewport) return;
    this.viewportX = 0;
    this.viewportY = 0;
    this.viewportWidth = width;
    this.viewportHeight = height;
  }

  setViewportRect(x: number, y: number, width: number, height: number): void {
    this.hasCustomViewport = true;
    this.viewportX = x;
    this.viewportY = y;
    this.viewportWidth = width;
    this.viewportHeight = height;
  }

  setCoordinate(coordinate: Coordinate): void {
    const changed = !isSameCoordinate(this.coordinate, coordinate);
    this.coordinate = coordinate;
    this.followedEntityId = null;
    if (changed) this.emitCoordinateChanged();
  }

  followEntity(networkId: number): void { this.followedEntityId = networkId === -1 ? null : networkId; }
  clearFollowedEntity(): void { this.followedEntityId = null; }

  updateFollowedCoordinate(resolveEntityCoordinate: (networkId: number) => Coordinate | null): boolean {
    if (this.followedEntityId === null) return false;
    const coordinate = resolveEntityCoordinate(this.followedEntityId);
    if (!coordinate) return false;
    const changed = !isSameCoordinate(this.coordinate, coordinate);
    this.coordinate = coordinate;
    if (changed) this.emitCoordinateChanged();
    return changed;
  }

  getCoordinate(): Coordinate { return { ...this.coordinate }; }
  getFollowedEntityId(): number | null { return this.followedEntityId; }
  getViewportRect(): { x: number; y: number; width: number; height: number } {
    return { x: this.viewportX, y: this.viewportY, width: this.viewportWidth, height: this.viewportHeight };
  }
  getWorldPosition(): { x: number; y: number } {
    const projected = projectCoordinate(this.coordinate);
    return {
      x: this.viewportX + this.viewportWidth / 2 - projected.x,
      y: this.viewportY + this.viewportHeight / 2 - projected.y,
    };
  }
  screenToWorld(screenX: number, screenY: number): { x: number; y: number } {
    const world = this.getWorldPosition();
    return { x: screenX - world.x, y: screenY - world.y };
  }
  addCoordinateChangedListener(listener: CameraCoordinateChangedListener): () => void {
    this.coordinateChangedListeners.add(listener);
    return () => this.coordinateChangedListeners.delete(listener);
  }
  private emitCoordinateChanged(): void {
    const coordinate = this.getCoordinate();
    for (const listener of [...this.coordinateChangedListeners]) listener(coordinate);
  }
}

function isSameCoordinate(left: Coordinate, right: Coordinate): boolean {
  return left.x === right.x && left.y === right.y && left.z === right.z;
}
