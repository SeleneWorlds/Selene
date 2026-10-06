import { projectCoordinate } from '@/core/WorldProjection';
import type { Coordinate } from '@/networking/GameProtocol';

export type CameraCoordinateChangedListener = (coordinate: Coordinate) => void;

export class ClientCamera {
  private readonly coordinateChangedListeners = new Set<CameraCoordinateChangedListener>();
  private coordinateEventsEnabled = true;
  private viewportWidth = 1;
  private viewportHeight = 1;
  private viewportX = 0;
  private viewportY = 0;
  private hasCustomViewport = false;
  private coordinate: Coordinate = { x: 0, y: 0, z: 0 };
  private followedEntityId: number | null = null;
  private offsetX = 0;
  private offsetY = 0;

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
    if (this.offsetX !== 0 || this.offsetY !== 0) {
      const previous = projectCoordinate(this.coordinate);
      const next = projectCoordinate(coordinate);
      this.offsetX += previous.x - next.x;
      this.offsetY += previous.y - next.y;
    }
    this.coordinate = coordinate;
    this.followedEntityId = null;
    if (changed) this.emitCoordinateChanged();
  }

  followEntity(networkId: number): void {
    this.followedEntityId = networkId === -1 ? null : networkId;
    this.offsetX = 0;
    this.offsetY = 0;
  }
  clearFollowedEntity(): void { this.followedEntityId = null; }

  updateFollowedCoordinate(resolveEntityCoordinate: (networkId: number) => Coordinate | null): boolean {
    if (this.followedEntityId === null) return false;
    const coordinate = resolveEntityCoordinate(this.followedEntityId);
    if (!coordinate) return false;
    const changed = !isSameCoordinate(this.coordinate, coordinate);
    const previousGridCoordinate = toGridCoordinate(this.coordinate);
    const nextGridCoordinate = toGridCoordinate(coordinate);
    this.coordinate = coordinate;
    if (!isSameCoordinate(previousGridCoordinate, nextGridCoordinate)) {
      this.emitCoordinateChanged(nextGridCoordinate);
    }
    return changed;
  }

  getCoordinate(): Coordinate { return { ...this.coordinate }; }
  getPosition(): { x: number; y: number } {
    const projected = projectCoordinate(this.coordinate);
    return { x: projected.x + this.offsetX, y: projected.y + this.offsetY };
  }
  setPosition(position: { x: number; y: number }): void {
    this.followedEntityId = null;
    const projected = projectCoordinate(this.coordinate);
    this.offsetX = position.x - projected.x;
    this.offsetY = position.y - projected.y;
  }
  getFollowedEntityId(): number | null { return this.followedEntityId; }
  getViewportRect(): { x: number; y: number; width: number; height: number } {
    return { x: this.viewportX, y: this.viewportY, width: this.viewportWidth, height: this.viewportHeight };
  }
  getWorldPosition(): { x: number; y: number } {
    const projected = projectCoordinate(this.coordinate);
    return {
      x: this.viewportX + this.viewportWidth / 2 - projected.x - this.offsetX,
      y: this.viewportY + this.viewportHeight / 2 - projected.y - this.offsetY,
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
  setCoordinateEventsEnabled(enabled: boolean): void { this.coordinateEventsEnabled = enabled; }
  private emitCoordinateChanged(coordinate = this.getCoordinate()): void {
    if (!this.coordinateEventsEnabled) return;
    for (const listener of [...this.coordinateChangedListeners]) listener(coordinate);
  }
}

function toGridCoordinate(coordinate: Coordinate): Coordinate {
  return {
    x: Math.round(coordinate.x),
    y: Math.round(coordinate.y),
    z: Math.round(coordinate.z),
  };
}

function isSameCoordinate(left: Coordinate, right: Coordinate): boolean {
  return left.x === right.x && left.y === right.y && left.z === right.z;
}
