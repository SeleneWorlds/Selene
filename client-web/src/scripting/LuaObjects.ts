import type { ClientDirection } from '@/api/GridApi';
import type { Coordinate } from '@/networking/GameProtocol';

export type LuaCoordinate = Coordinate & {
  getX: () => number;
  getY: () => number;
  getZ: () => number;
  getHorizontalDistanceTo: (other: Coordinate) => number;
  offset: (direction: ClientDirection) => LuaCoordinate;
};

export type LuaDirection = ClientDirection & {
  getName: () => string;
  getVector: () => LuaCoordinate;
  getAngle: () => number;
};

export function toLuaCoordinate(coordinate: Coordinate): LuaCoordinate {
  return {
    ...coordinate,
    getX: () => coordinate.x,
    getY: () => coordinate.y,
    getZ: () => coordinate.z,
    getHorizontalDistanceTo: (other) => {
      const dx = coordinate.x - other.x;
      const dy = coordinate.y - other.y;

      return Math.trunc(Math.sqrt(dx * dx + dy * dy));
    },
    offset: (direction) => toLuaCoordinate({
      x: coordinate.x + direction.vector.x,
      y: coordinate.y + direction.vector.y,
      z: coordinate.z + direction.vector.z,
    }),
  };
}

export function toLuaDirection(direction: ClientDirection): LuaDirection {
  const vector = toLuaCoordinate(direction.vector);

  return {
    name: direction.name,
    vector,
    angle: direction.angle,
    getName: () => direction.name,
    getVector: () => vector,
    getAngle: () => direction.angle,
  };
}
