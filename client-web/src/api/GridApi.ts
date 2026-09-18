import type { Coordinate } from '@/networking/GameProtocol';
export type { ClientGridDefinition, ClientGridDirectionDefinition } from '@/data/ClientRegistrySchemas';

export interface ClientDirection {
  name: string;
  vector: Coordinate;
  angle: number;
}

export interface GridApi {
  defineDirection: (name: string, x: number, y: number, z: number, angle: number) => Coordinate;
  getDirectionByName: (name: string) => ClientDirection;
  getDirection: (angle: number) => ClientDirection | null;
  screenToCoordinate: (x: number, y: number, z?: number) => Coordinate;
}
