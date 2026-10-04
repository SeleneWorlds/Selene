import type { LightColor, TileLight } from '@/api/EnvironmentApi';
import type { Coordinate } from '@/networking/GameProtocol';

export interface ResolvedLightColor extends LightColor {}

interface LightSource extends TileLight {
  coordinate: Coordinate;
  intensity: number;
}

const WHITE: LightColor = { red: 1, green: 1, blue: 1 };

export class LightingEnvironment {
  private ambient: LightColor = { ...WHITE };
  private readonly tileDefinitionLights = new Map<string, LightSource>();
  private readonly explicitLights = new Map<string, LightSource>();
  private readonly entityLights = new Map<number, LightSource>();
  private revision = 0;

  getRevision(): number { return this.revision; }

  getAmbientLight(): LightColor { return { ...this.ambient }; }

  setAmbientLight(color: LightColor): void {
    const next = normalizeColor(color);
    if (sameColor(this.ambient, next)) return;
    this.ambient = next;
    this.revision += 1;
  }

  setTileDefinitionLight(coordinate: Coordinate, value: TileLight | null): void {
    const key = coordinateKey(coordinate);
    const light = parseLight(value);
    if (light) this.tileDefinitionLights.set(key, { coordinate: { ...coordinate }, ...light });
    else this.tileDefinitionLights.delete(key);
    this.revision += 1;
  }

  removeTileDefinitionLight(coordinate: Coordinate): void {
    if (this.tileDefinitionLights.delete(coordinateKey(coordinate))) this.revision += 1;
  }

  setTileLight(coordinate: Coordinate, value: TileLight | null): void {
    const key = coordinateKey(coordinate);
    const light = parseLight(value);
    if (light) this.explicitLights.set(key, { coordinate: { ...coordinate }, ...light });
    else this.explicitLights.delete(key);
    this.revision += 1;
  }

  clearTileLights(): void {
    if (this.explicitLights.size === 0) return;
    this.explicitLights.clear();
    this.revision += 1;
  }

  setEntityLight(id: number, coordinate: Coordinate, value: TileLight | null): void {
    const light = parseLight(value);
    const previous = this.entityLights.get(id);
    if (!light) {
      if (this.entityLights.delete(id)) this.revision += 1;
      return;
    }
    if (previous && sameCoordinate(previous.coordinate, coordinate) && sameLight(previous, light)) return;
    this.entityLights.set(id, { coordinate: { ...coordinate }, ...light });
    this.revision += 1;
  }

  removeEntityLight(id: number): void {
    if (this.entityLights.delete(id)) this.revision += 1;
  }

  getColor(coordinate: Coordinate): ResolvedLightColor {
    let red = this.ambient.red;
    let green = this.ambient.green;
    let blue = this.ambient.blue;
    const ambientLuminosity = (red + green + blue) / 3;
    const localScale = 1 - ambientLuminosity;

    for (const source of this.sources()) {
      if (source.coordinate.z !== Math.round(coordinate.z)) continue;
      const distance = Math.hypot(coordinate.x - source.coordinate.x, coordinate.y - source.coordinate.y);
      if (distance > source.radius) continue;
      const falloff = Math.max(0, 1 - distance / (source.radius + 0.5));
      const amount = falloff * source.intensity * localScale;
      red += source.red * amount;
      green += source.green * amount;
      blue += source.blue * amount;
    }
    return { red: clamp(red), green: clamp(green), blue: clamp(blue) };
  }

  private *sources(): Iterable<LightSource> {
    yield* this.tileDefinitionLights.values();
    yield* this.explicitLights.values();
    yield* this.entityLights.values();
  }
}

function parseLight(value: unknown): Omit<LightSource, 'coordinate'> | null {
  if (!isRecord(value)) return null;
  const radius = finite(value.radius ?? value.size);
  if (radius === null || radius <= 0) return null;
  const color = isRecord(value.color) ? value.color : value;
  return {
    radius: Math.min(32, radius),
    intensity: Math.max(0, finite(value.intensity ?? value.brightness) ?? 1),
    red: clamp(finite(color.red ?? color.r) ?? 1),
    green: clamp(finite(color.green ?? color.g) ?? 1),
    blue: clamp(finite(color.blue ?? color.b) ?? 1),
  };
}

function normalizeColor(value: LightColor): LightColor {
  return { red: clamp(value.red), green: clamp(value.green), blue: clamp(value.blue) };
}
function clamp(value: number): number { return Math.max(0, Math.min(1, Number.isFinite(value) ? value : 1)); }
function finite(value: unknown): number | null { return typeof value === 'number' && Number.isFinite(value) ? value : null; }
function isRecord(value: unknown): value is Record<string, unknown> { return typeof value === 'object' && value !== null && !Array.isArray(value); }
function sameColor(a: LightColor, b: LightColor): boolean { return a.red === b.red && a.green === b.green && a.blue === b.blue; }
function sameCoordinate(a: Coordinate, b: Coordinate): boolean { return a.x === b.x && a.y === b.y && a.z === b.z; }
function sameLight(a: TileLight, b: TileLight): boolean {
  return a.radius === b.radius && a.intensity === b.intensity && sameColor(a, b);
}
function coordinateKey(value: Coordinate): string { return `${value.x}:${value.y}:${value.z}`; }
