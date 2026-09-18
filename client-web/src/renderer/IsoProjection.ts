import { projectCoordinate } from '@/core/WorldProjection';
import type { Coordinate } from '@/networking/GameProtocol';

export { projectCoordinate };

export const TILE_WIDTH = 76;
export const TILE_HEIGHT = 38;
export const Z_SORT_SCALE = 500;
export const ROW_SORT_SCALE = 50;
export const ENTITY_LOCAL_SORT_LAYER = 0.5;

export function getSortLayer(coordinate: Coordinate, sortLayerOffset = 0): number {
  return ((coordinate.x - coordinate.y - coordinate.z * Z_SORT_SCALE) * ROW_SORT_SCALE) - sortLayerOffset;
}

export function getRenderOrder(coordinate: Coordinate, sortLayerOffset = 0, localSortLayer = 0): number {
  return -getSortLayer(coordinate, sortLayerOffset) * 1000 + localSortLayer;
}
