import { Container, Graphics } from 'pixi.js';
import { getRenderOrder, projectCoordinate, TILE_WIDTH, TILE_HEIGHT } from './IsoProjection';
import { TILE_STEP_X, TILE_STEP_Y, TILE_STEP_Z } from '@/core/WorldProjection';
import type { Coordinate } from '@/networking/GameProtocol';
import type { WorldBounds } from './entities/PixiEntityLayer';

/** Ground-plane cells share the scene's depth sorting with tiles and entities. */
export class PixiTileGrid {
  private visible = false;
  private readonly cells = new Map<string, Graphics>();

  constructor(
    private readonly parent: Container,
    private readonly getGroundRenderOrder: (coordinate: Coordinate) => number = getRenderOrder,
  ) {}

  setVisible(visible: boolean): void {
    this.visible = visible;
    if (!visible) this.clear();
  }

  update(bounds: WorldBounds, z: number): void {
    if (!this.visible) return;
    const corners = [
      [bounds.x, bounds.y], [bounds.x + bounds.width, bounds.y],
      [bounds.x, bounds.y + bounds.height], [bounds.x + bounds.width, bounds.y + bounds.height],
    ].map(([x, y]) => {
      const difference = (-y - z * TILE_STEP_Z) / TILE_STEP_Y;
      return { x: (x / TILE_STEP_X + difference) / 2, y: (x / TILE_STEP_X - difference) / 2 };
    });
    const minX = Math.floor(Math.min(...corners.map(c => c.x))) - 1;
    const maxX = Math.ceil(Math.max(...corners.map(c => c.x))) + 1;
    const minY = Math.floor(Math.min(...corners.map(c => c.y))) - 1;
    const maxY = Math.ceil(Math.max(...corners.map(c => c.y))) + 1;
    const needed = new Set<string>();
    for (let x = minX; x <= maxX; x++) {
      for (let y = minY; y <= maxY; y++) {
        const coordinate: Coordinate = { x, y, z };
        const position = projectCoordinate(coordinate);
        if (position.x + TILE_WIDTH / 2 < bounds.x || position.x - TILE_WIDTH / 2 > bounds.x + bounds.width ||
            position.y + TILE_HEIGHT / 2 < bounds.y || position.y - TILE_HEIGHT / 2 > bounds.y + bounds.height) continue;
        const key = `${x},${y},${z}`;
        needed.add(key);
        const renderOrder = this.getGroundRenderOrder(coordinate) + 0.25;
        const existing = this.cells.get(key);
        if (existing) {
          existing.zIndex = renderOrder;
          continue;
        }
        const cell = new Graphics()
          .moveTo(0, -TILE_HEIGHT / 2).lineTo(TILE_WIDTH / 2, 0)
          .lineTo(0, TILE_HEIGHT / 2).lineTo(-TILE_WIDTH / 2, 0).closePath()
          .stroke({ color: 0xffffff, alpha: 0.4, width: 1 });
        cell.position.set(position.x, position.y);
        // Draw above flat ground and transitions, below entities and raised sprites.
        cell.zIndex = renderOrder;
        this.parent.addChild(cell);
        this.cells.set(key, cell);
      }
    }
    for (const [key, cell] of this.cells) {
      if (!needed.has(key)) {
        cell.destroy();
        this.cells.delete(key);
      }
    }
  }

  private clear(): void {
    for (const cell of this.cells.values()) cell.destroy();
    this.cells.clear();
  }
}
