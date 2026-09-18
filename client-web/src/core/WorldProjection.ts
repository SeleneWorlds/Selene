import type { Coordinate } from '@/networking/GameProtocol';

export const TILE_STEP_X = 38;
export const TILE_STEP_Y = 19;
export const TILE_STEP_Z = 114;

export function projectCoordinate(coordinate: Coordinate): { x: number; y: number } {
  return {
    x: (coordinate.x + coordinate.y) * TILE_STEP_X,
    y: -((coordinate.x - coordinate.y) * TILE_STEP_Y + coordinate.z * TILE_STEP_Z),
  };
}
