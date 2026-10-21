import type { Coordinate } from '@/networking/GameProtocol';

import { TILE_STEP_X, TILE_STEP_Y, TILE_STEP_Z } from './RenderGrid';
export { TILE_STEP_X, TILE_STEP_Y, TILE_STEP_Z };

export function projectCoordinate(coordinate: Coordinate): { x: number; y: number } {
  return {
    x: (coordinate.x + coordinate.y) * TILE_STEP_X,
    y: -((coordinate.x - coordinate.y) * TILE_STEP_Y + coordinate.z * TILE_STEP_Z),
  };
}
