import type { MapApi } from '@/api/MapApi';
import type { RegistriesApi } from '@/api/RegistriesApi';
import type { VisualsApi } from '@/api/VisualsApi';
import type { Coordinate } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import type { ClientMapTile } from '@/core/ClientMap';
import { RegistryObject } from './RegistriesService';

export interface MapServiceOptions {
  nameIdMappings: NameIdMappings;
  getMapTiles(): ClientMapTile[];
  onMapChanged(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void;
}

export class MapService implements MapApi {
  constructor(private readonly options: MapServiceOptions, private readonly registries: RegistriesApi, private readonly visuals: VisualsApi) {}
  getTilesAt(coordinate: Coordinate) {
    const stack = this.options.getMapTiles().find((tile) => sameCoordinate(tile, coordinate));
    return stack?.tileIds.map((id) => {
      const name = this.options.nameIdMappings.getName('tiles', id) ?? `unknown:${id}`;
      const definition = this.registries.findByName('tiles', name) ?? new RegistryObject(name, {}, id);
      const visualName = typeof definition.visual === 'string' ? definition.visual : null;
      return { getCoordinate: () => ({ ...coordinate }), getDefinition: () => definition,
        getVisual: () => visualName ? this.visuals.create(visualName) : null, getX: () => coordinate.x,
        getY: () => coordinate.y, getZ: () => coordinate.z, getName: () => name };
    }) ?? [];
  }
  hasTileAt(coordinate: Coordinate) { return this.getTilesAt(coordinate).length > 0; }
  addChunkChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void) { return this.options.onMapChanged(listener); }
}

function sameCoordinate(a: Coordinate, b: Coordinate): boolean { return a.x === b.x && a.y === b.y && a.z === b.z; }
