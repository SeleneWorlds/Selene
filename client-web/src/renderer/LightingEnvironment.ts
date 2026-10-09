import type { LightColor, TileLight } from '@/api/EnvironmentApi';
import type { Coordinate } from '@/networking/GameProtocol';

export interface ResolvedLightColor extends LightColor {}

interface LightSource extends TileLight {
  coordinate: Coordinate;
  intensity: number;
}

const LIGHT_BUCKET_SIZE = 16;

const WHITE: LightColor = { red: 1, green: 1, blue: 1 };

export class LightingEnvironment {
  private ambient: LightColor = { ...WHITE };
  private readonly tileDefinitionLights = new Map<string, LightSource>();
  private readonly explicitLights = new Map<string, LightSource>();
  private readonly entityLights = new Map<number, LightSource>();
  private readonly sourceBuckets = new Map<string, Set<LightSource>>();
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
    this.setSource(this.tileDefinitionLights, coordinateKey(coordinate), coordinate, value);
  }

  removeTileDefinitionLight(coordinate: Coordinate): void {
    this.removeSource(this.tileDefinitionLights, coordinateKey(coordinate));
  }

  setTileLight(coordinate: Coordinate, value: TileLight | null): void {
    this.setSource(this.explicitLights, coordinateKey(coordinate), coordinate, value);
  }

  clearTileLights(): void {
    if (this.explicitLights.size === 0) return;
    for (const source of this.explicitLights.values()) this.indexSource(source, false);
    this.explicitLights.clear();
    this.revision += 1;
  }

  setEntityLight(id: number, coordinate: Coordinate, value: TileLight | null): void {
    this.setSource(this.entityLights, id, coordinate, value);
  }

  removeEntityLight(id: number): void {
    this.removeSource(this.entityLights, id);
  }

  private setSource<TKey>(sources: Map<TKey, LightSource>, key: TKey, coordinate: Coordinate, value: TileLight | null): void {
    const light = parseLight(value);
    const previous = sources.get(key);
    if (!light) {
      this.removeSource(sources, key);
      return;
    }
    if (previous && sameCoordinate(previous.coordinate, coordinate) && sameLight(previous, light)) return;
    if (previous) this.indexSource(previous, false);
    const source = { coordinate: { ...coordinate }, ...light };
    sources.set(key, source);
    this.indexSource(source, true);
    this.revision += 1;
  }

  private removeSource<TKey>(sources: Map<TKey, LightSource>, key: TKey): void {
    const previous = sources.get(key);
    if (!previous) return;
    this.indexSource(previous, false);
    sources.delete(key);
    this.revision += 1;
  }

  private indexSource(source: LightSource, add: boolean): void {
    const { coordinate, radius } = source;
    const minX = Math.floor((coordinate.x - radius) / LIGHT_BUCKET_SIZE);
    const maxX = Math.floor((coordinate.x + radius) / LIGHT_BUCKET_SIZE);
    const minY = Math.floor((coordinate.y - radius) / LIGHT_BUCKET_SIZE);
    const maxY = Math.floor((coordinate.y + radius) / LIGHT_BUCKET_SIZE);
    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const key = `${x}:${y}:${coordinate.z}`;
        if (add) {
          let bucket = this.sourceBuckets.get(key);
          if (!bucket) this.sourceBuckets.set(key, bucket = new Set());
          bucket.add(source);
        } else {
          const bucket = this.sourceBuckets.get(key);
          bucket?.delete(source);
          if (bucket?.size === 0) this.sourceBuckets.delete(key);
        }
      }
    }
  }

  getColor(coordinate: Coordinate): ResolvedLightColor {
    let red = this.ambient.red;
    let green = this.ambient.green;
    let blue = this.ambient.blue;
    const ambientLuminosity = (red + green + blue) / 3;
    const localScale = 1 - ambientLuminosity;

    if (localScale === 0) return { red, green, blue };
    const bucketKey = `${Math.floor(coordinate.x / LIGHT_BUCKET_SIZE)}:${Math.floor(coordinate.y / LIGHT_BUCKET_SIZE)}:${Math.round(coordinate.z)}`;
    const sources = this.sourceBuckets.get(bucketKey);
    if (!sources) return { red, green, blue };
    for (const source of sources) {
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
