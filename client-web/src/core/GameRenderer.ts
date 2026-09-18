import type { ClientEntitySnapshot } from '@/api/EntitiesApi';
import type { ClientCamera } from '@/core/ClientCamera';
import type { ClientGrid } from '@/core/ClientGrid';
import type { ClientMapTile } from '@/core/ClientMap';
import type { RendererDebugOptions } from '@/core/DebugState';
import type { Coordinate, GamePacket } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';

export interface GameRendererContext {
  camera: ClientCamera;
  grid: ClientGrid;
  nameIdMappings: NameIdMappings;
}

export type GameRendererFactory = (context: GameRendererContext) => GameRenderer;

export interface GameRenderer {
  initialize(): Promise<void>;
  update(deltaMs: number): Record<string, number>;
  handlePacket(packet: GamePacket): void;
  drawCameraScene(): void;
  upsertClientEntity(snapshot: ClientEntitySnapshot): void;
  setClientEntityAlpha(id: number, alpha: number): void;
  removeClientEntity(id: number): void;
  setWindowAspectRatio(width: number, height: number): void;
  clearWindowAspectRatio(): void;
  setOffscreenRendering(width: number, height: number): void;
  setNativeRendering(): void;
  setFitToScreen(enabled: boolean): void;
  setDebugOption(option: keyof RendererDebugOptions, enabled: boolean): void;
  screenToLogical(screenX: number, screenY: number): { x: number; y: number };
  getMapTiles(): ClientMapTile[];
  addMapChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void;
}
