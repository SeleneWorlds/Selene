import type { Coordinate } from '@/networking/GameProtocol';

export interface LightColor {
  red: number;
  green: number;
  blue: number;
}

export interface TileLight extends LightColor {
  radius: number;
  intensity?: number;
}

export interface EnvironmentApi {
  setAmbientLight(name: string, color: LightColor): void;
  getAmbientLight(name: string): LightColor;
  setTileLight(coordinate: Coordinate, light: TileLight | null): void;
  clearTileLights(): void;
}
