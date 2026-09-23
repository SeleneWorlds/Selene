import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type { Coordinate } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import type { ClientVisualDefinition } from '@/data/ClientRegistrySchemas';

export interface ResolvedTileVisual {
  texturePath: string;
  animation: {
    textures: string[];
    duration: number;
    instanced: boolean;
  } | null;
  sortLayerOffset: number;
  surfaceHeight: number;
  offsetX: number;
  offsetY: number;
  flipX: boolean;
  flipY: boolean;
  metadata: Readonly<Record<string, unknown>>;
}

export class TileVisualResolver {
  private readonly warnedReasons = new Set<string>();

  constructor(
    private readonly registries: ClientRegistrySnapshots,
    private readonly nameIdMappings: NameIdMappings,
  ) {}

  resolve(tileId: number, coordinate: Coordinate): ResolvedTileVisual | null {
    const tileName = this.nameIdMappings.getName('tiles', tileId);
    if (!tileName) {
      this.warnOnce(`mapping:${tileId}`, `No tile id mapping for id ${tileId}.`);
      return null;
    }

    const tileDefinition = getRegistryEntry(this.registries, 'tiles', tileName);
    if (!tileDefinition?.visual) {
      this.warnOnce(`tile:${tileName}`, `Tile ${tileName} has no visual.`);
      return null;
    }

    const visualDefinition = getRegistryEntry(this.registries, 'visuals', tileDefinition.visual);
    if (!visualDefinition) {
      this.warnOnce(`visual:${tileDefinition.visual}`, `Tile ${tileName} references missing visual ${tileDefinition.visual}.`);
      return null;
    }

    const animation = resolveAnimation(visualDefinition);
    const texturePath = animation?.textures[0] ?? selectTexturePath(visualDefinition, coordinate);
    if (!texturePath) {
      this.warnOnce(`texture:${tileDefinition.visual}`, `Tile visual ${tileDefinition.visual} has no supported texture.`);
      return null;
    }

    return {
      texturePath,
      animation,
      sortLayerOffset: visualDefinition.sortLayerOffset ?? 0,
      surfaceHeight: visualDefinition.surfaceOffsetY ?? 0,
      offsetX: visualDefinition.offsetX ?? 0,
      offsetY: visualDefinition.offsetY ?? 0,
      flipX: visualDefinition.flipX ?? false,
      flipY: visualDefinition.flipY ?? false,
      metadata: { ...(visualDefinition.metadata ?? {}) },
    };
  }

  private warnOnce(key: string, message: string): void {
    if (this.warnedReasons.has(key)) {
      return;
    }

    this.warnedReasons.add(key);
    console.warn(`[Tiles] ${message}`);
  }
}

function selectTexturePath(visualDefinition: ClientVisualDefinition, coordinate: Coordinate): string | null {
  if (typeof visualDefinition.texture === 'string') {
    return visualDefinition.texture;
  }

  if (Array.isArray(visualDefinition.textures) && visualDefinition.textures.length > 0) {
    const variantIndex = Math.abs(coordinate.x * 31 + coordinate.y * 17 + coordinate.z * 13) % visualDefinition.textures.length;
    return visualDefinition.textures[variantIndex] ?? null;
  }

  const firstAnimation = Object.values(visualDefinition.animations ?? {})[0];
  return firstAnimation?.textures?.[0] ?? null;
}

function resolveAnimation(visualDefinition: ClientVisualDefinition): ResolvedTileVisual['animation'] {
  if (visualDefinition.type !== 'animated') {
    return null;
  }

  const textures = visualDefinition.textures?.filter((texture): texture is string => typeof texture === 'string') ?? [];
  if (textures.length === 0) {
    return null;
  }

  return {
    textures,
    duration: Math.max(visualDefinition.duration ?? 1, Number.EPSILON),
    instanced: visualDefinition.instanced ?? false,
  };
}
