import { projectCoordinate } from '@/core/WorldProjection';
import type { Coordinate } from '@/networking/GameProtocol';
import { Z_SORT_SCALE, ROW_SORT_SCALE } from '@/core/RenderGrid';

export { projectCoordinate };

export { TILE_WIDTH, TILE_HEIGHT, Z_SORT_SCALE, ROW_SORT_SCALE } from '@/core/RenderGrid';
export const ENTITY_LOCAL_SORT_LAYER = 0.5;

export function getSortLayer(coordinate: Coordinate, sortLayerOffset = 0): number {
  return ((coordinate.x - coordinate.y - coordinate.z * Z_SORT_SCALE) * ROW_SORT_SCALE) - sortLayerOffset;
}

export function getRenderOrder(coordinate: Coordinate, sortLayerOffset = 0, localSortLayer = 0): number {
  return -getSortLayer(coordinate, sortLayerOffset) * 1000 + localSortLayer;
}
