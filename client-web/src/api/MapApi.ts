import type { Coordinate } from '@/networking/GameProtocol';
import type { RegistryObjectApi } from './RegistriesApi';
import type { VisualApi } from './VisualsApi';
export interface TileApi { getCoordinate(): Coordinate; getDefinition(): RegistryObjectApi; getVisual(): VisualApi | null; getX(): number; getY(): number; getZ(): number; getName(): string }
export interface MapApi { getTilesAt(coordinate: Coordinate): TileApi[]; hasTileAt(coordinate: Coordinate): boolean; addChunkChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void }
