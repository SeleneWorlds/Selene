import type { Container } from 'pixi.js';
import type { ClientMapTile } from '@/core/ClientMap';
import type { Coordinate, MapChunkPacket, RemoveMapChunkPacket, UpdateMapTilesPacket } from '@/networking/GameProtocol';
import type { WorldBounds } from '../entities/PixiEntityLayer';
import type { ContentTextureLoader } from '../entities/ContentTextureLoader';
import type { RenderedTileStack } from './RenderedTileStack';
import { PixiTileStackRenderer } from './PixiTileStackRenderer';
import type { TileVisualResolver } from './TileVisualResolver';
import { TileCuller } from './TileCuller';
import { TileOcclusionFader } from './TileOcclusionFader';
import { TileSpatialIndex } from './TileSpatialIndex';

/** Coordinates map state and delegates tile rendering and visibility policy. */
export class PixiTilemapLayer {
  private readonly renderedStacks = new Map<string, RenderedTileStack>();
  private readonly mapTiles = new Map<string, ClientMapTile>();
  private readonly mapChangedListeners = new Set<(coordinate: Coordinate, width: number, height: number) => void>();
  private readonly spatialIndex = new TileSpatialIndex();
  private readonly culler = new TileCuller(this.spatialIndex);
  private readonly occlusionFader = new TileOcclusionFader(this.spatialIndex);
  private readonly stackRenderer: PixiTileStackRenderer;
  private generation = 0;
  private upperLayerFocusZ: number | null = null;
  private upperLayerAlpha: number | null = null;
  private upperLayerDirty = false;

  constructor(
    private readonly visualResolver: TileVisualResolver,
    textureLoader: ContentTextureLoader,
    readonly container: Container,
  ) {
    this.stackRenderer = new PixiTileStackRenderer(
      visualResolver,
      textureLoader,
      container,
      (stack, index) => {
        this.spatialIndex.refreshBounds(stack, index);
        this.culler.boundsChanged(stack);
      },
    );
  }

  setMapChunk(packet: MapChunkPacket): void {
    for (let dy = 0; dy < packet.height; dy += 1) {
      for (let dx = 0; dx < packet.width; dx += 1) {
        const coordinate = { x: packet.x + dx, y: packet.y + dy, z: packet.z };
        const baseTileId = packet.baseTiles[dy * packet.width + dx] ?? 0;
        this.setTileStack(coordinate, baseTileId === 0 ? [] : [baseTileId]);
      }
    }
    for (const tile of packet.additionalTiles) {
      if (tile.tileId !== 0) this.appendTile(tile.coordinate, tile.tileId);
    }
    this.notifyMapChanged({ x: packet.x, y: packet.y, z: packet.z }, packet.width, packet.height);
  }

  removeMapChunk(packet: RemoveMapChunkPacket): void {
    for (let dy = 0; dy < packet.height; dy += 1) {
      for (let dx = 0; dx < packet.width; dx += 1) {
        this.removeTileStack({ x: packet.x + dx, y: packet.y + dy, z: packet.z });
      }
    }
    this.notifyMapChanged({ x: packet.x, y: packet.y, z: packet.z }, packet.width, packet.height);
  }

  updateMapTiles(packet: UpdateMapTilesPacket): void {
    this.setTileStack(packet.coordinate, [
      ...(packet.baseTileId === 0 ? [] : [packet.baseTileId]),
      ...packet.additionalTileIds.filter(tileId => tileId !== 0),
    ]);
    this.notifyMapChanged(packet.coordinate, 1, 1);
  }

  getMapTiles(): ClientMapTile[] {
    return [...this.mapTiles.values()].map(tile => ({
      ...tile,
      tileIds: [...tile.tileIds],
      visualMetadata: { ...tile.visualMetadata },
    }));
  }

  addMapChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void {
    this.mapChangedListeners.add(listener);
    return () => this.mapChangedListeners.delete(listener);
  }

  updateAnimations(deltaMs: number): void {
    this.stackRenderer.updateAnimations(deltaMs, stack => this.isCurrent(stack));
  }

  updateOcclusion(deltaMs: number, focusCoordinate: Coordinate, focusBounds: WorldBounds): void {
    this.occlusionFader.update(deltaMs, focusCoordinate, focusBounds);
  }

  setCullingEnabled(enabled: boolean): void {
    this.culler.setEnabled(enabled, this.renderedStacks.values());
  }

  updateCulling(view: WorldBounds): void {
    this.culler.update(view);
  }

  hasTiles(): boolean { return this.renderedStacks.size > 0; }

  hasTileAt(coordinate: Coordinate): boolean { return this.renderedStacks.has(coordinateKey(coordinate)); }

  getSurfaceHeight(coordinate: Coordinate, beforeRenderOrder: number): number {
    return this.renderedStacks.get(coordinateKey(coordinate))?.surfaces.reduce(
      (height, surface) => surface.renderOrder < beforeRenderOrder ? height + surface.height : height,
      0,
    ) ?? 0;
  }

  setUpperLayerAlpha(focusZ: number, alpha: number): void {
    if (!this.upperLayerDirty && this.upperLayerFocusZ === focusZ && this.upperLayerAlpha === alpha) return;
    this.upperLayerFocusZ = focusZ;
    this.upperLayerAlpha = alpha;
    this.upperLayerDirty = false;
    this.occlusionFader.setUpperLayerAlpha(focusZ, alpha, this.renderedStacks.values());
  }

  private appendTile(coordinate: Coordinate, tileId: number): void {
    const existing = this.renderedStacks.get(coordinateKey(coordinate));
    this.setTileStack(coordinate, [...(existing?.tileIds ?? []), tileId]);
  }

  private setTileStack(coordinate: Coordinate, tileIds: readonly number[]): void {
    this.removeTileStack(coordinate);
    const key = coordinateKey(coordinate);
    const baseTileId = tileIds[0];
    if (baseTileId === undefined) {
      this.mapTiles.delete(key);
      return;
    }
    const visualMetadata = this.visualResolver.resolve(baseTileId, coordinate)?.metadata ?? {};
    this.mapTiles.set(key, { ...coordinate, tileIds: [...tileIds], visualMetadata: { ...visualMetadata } });
    const stack = this.stackRenderer.createStack(coordinate, tileIds, ++this.generation);
    this.renderedStacks.set(key, stack);
    this.culler.add(stack);
    this.upperLayerDirty = true;
    void this.stackRenderer.renderStack(stack, candidate => this.isCurrent(candidate));
  }

  private removeTileStack(coordinate: Coordinate): void {
    const key = coordinateKey(coordinate);
    const stack = this.renderedStacks.get(key);
    if (!stack) return;
    stack.generation = -1;
    this.culler.remove(stack);
    this.occlusionFader.remove(stack);
    this.spatialIndex.remove(stack);
    this.stackRenderer.removeStack(stack);
    this.renderedStacks.delete(key);
  }

  private isCurrent(stack: RenderedTileStack): boolean {
    return this.renderedStacks.get(coordinateKey(stack.coordinate)) === stack;
  }

  private notifyMapChanged(coordinate: Coordinate, width: number, height: number): void {
    for (const listener of [...this.mapChangedListeners]) listener(coordinate, width, height);
  }
}

function coordinateKey(coordinate: Coordinate): string {
  return `${coordinate.x}:${coordinate.y}:${coordinate.z}`;
}
