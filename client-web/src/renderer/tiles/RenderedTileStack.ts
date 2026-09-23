import type { Container, Sprite } from 'pixi.js';
import type { Coordinate } from '@/networking/GameProtocol';
import type { WorldBounds } from '../entities/PixiEntityLayer';
import type { ResolvedTileVisual } from './TileVisualResolver';

export interface RenderedAnimatedTile {
  sprite: Sprite;
  visual: ResolvedTileVisual;
  elapsedMs: number;
  frameIndex: number;
  revision: number;
}

export interface RenderedTileStack {
  containers: Container[];
  surfaceOffsets: number[];
  surfaces: { renderOrder: number; height: number }[];
  localBounds: (WorldBounds | null)[];
  coordinate: Coordinate;
  tileIds: readonly number[];
  generation: number;
  animatedTiles: RenderedAnimatedTile[];
  occlusionAlphas: number[];
  targetOcclusionAlphas: number[];
  upperLayerAlpha: number;
  cullingBounds: WorldBounds | null;
  cullingBucketKeys: string[];
}
