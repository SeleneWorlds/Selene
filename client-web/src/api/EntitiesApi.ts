import type { Coordinate } from '@/networking/GameProtocol';
import type { ClientEntityDefinition } from '@/data/ClientRegistrySchemas';
export type { ClientEntityDefinition } from '@/data/ClientRegistrySchemas';

export interface ClientEntitySnapshot {
  id: number;
  networkId: number;
  entityName: string;
  definition: ClientEntityDefinition;
  coordinate: Coordinate;
  facing: number;
  components: Record<string, unknown>;
  spawned: boolean;
}

export interface EntityApi {
  getNetworkId: () => number;
  getCoordinate: () => Coordinate;
  getFacing: () => number;
  getDefinitionName: () => string;
  spawn: () => void;
  despawn: () => void;
  setCoordinate: (coordinate: Coordinate) => void;
  addComponent: (name: string, componentData: unknown) => void;
  setComponentAlpha: (name: string, alpha: number) => void;
  setComponentProperty: (name: string, property: string, value: unknown) => void;
  getComponent: (name: string) => unknown;
  getDefinition: () => ClientEntityDefinition;
}

export type ClientEntityScriptRunner = (
  scriptModule: string,
  entityId: number,
  entity: EntityApi,
  deltaSeconds: number,
) => void;

export interface EntitiesApi {
  create: (entityDefinition: string | ClientEntityDefinition) => EntityApi;
  getEntityByNetworkId: (networkId: number) => EntityApi | null;
  getEntitiesAt: (coordinate: Coordinate) => EntityApi[];
  findEntitiesAt: (coordinate: Coordinate, criteria?: { tag?: string }) => EntityApi[];
}

export interface ClientEntitiesListener {
  entityChanged: (snapshot: ClientEntitySnapshot) => void;
  entityRemoved: (id: number) => void;
  entityVisualAlphaChanged: (id: number, alpha: number) => void;
}
