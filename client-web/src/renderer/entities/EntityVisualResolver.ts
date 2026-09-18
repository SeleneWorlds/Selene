import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type { ClientEntityDefinition } from '@/api/EntitiesApi';
import type { EntityPacket } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import type { ClientVisualDefinition } from '@/data/ClientRegistrySchemas';

export interface ResolvedEntityVisual {
  texturePath?: string | undefined;
  animations?: Record<string, ResolvedEntityAnimation> | undefined;
  sortLayerOffset: number;
  offsetX: number;
  offsetY: number;
  alpha: number;
  red: number;
  green: number;
  blue: number;
  flipX: boolean;
  flipY: boolean;
  nameTag?: ResolvedEntityNameTag | undefined;
}

export interface ResolvedEntityNameTag {
  text: string;
  offsetX: number;
  offsetY: number;
}

export interface ResolvedEntityAnimation {
  textures: string[];
  frameDuration: number;
  offsetX: number;
  offsetY: number;
  flipX: boolean;
  flipY: boolean;
}

interface VisualComponentConfiguration {
  type?: string;
  visual?: string;
  alpha?: number;
  red?: number;
  green?: number;
  blue?: number;
  position?: {
    origin?: string;
    offsetX?: number;
    offsetY?: number;
  };
  overrides?: {
    text?: unknown;
  };
}

export class EntityVisualResolver {
  private readonly warnedReasons = new Set<string>();

  constructor(
    private readonly registries: ClientRegistrySnapshots,
    private readonly nameIdMappings: NameIdMappings,
  ) {}

  resolve(packet: EntityPacket): ResolvedEntityVisual | null {
    const entityName = this.nameIdMappings.getName('entities', packet.entityId);
    if (!entityName) {
      this.warnOnce(`mapping:${packet.entityId}`, `No entity id mapping for id ${packet.entityId}.`);
      return null;
    }

    const entityDefinition = getRegistryEntry(this.registries, 'entities', entityName);
    if (!entityDefinition) {
      this.warnOnce(`entity:${entityName}`, `No entity registry entry for ${entityName}.`);
      return null;
    }

    return this.resolveDefinition(entityName, entityDefinition, packet.components);
  }

  resolveDefinition(
    entityName: string,
    entityDefinition: ClientEntityDefinition,
    componentOverrides: Record<string, unknown> = {},
  ): ResolvedEntityVisual | null {
    const components = Object.values({
      ...(entityDefinition.components ?? {}),
      ...componentOverrides,
    }).map(parseComponentConfiguration);
    const visualComponents = components.filter(isVisualComponent);
    const visualComponent = visualComponents.find((component) => {
      if (!component.visual) {
        return false;
      }
      return getRegistryEntry(this.registries, 'visuals', component.visual)?.type !== 'text';
    });
    if (!visualComponent?.visual) {
      this.warnOnce(`component:${entityName}`, `Entity ${entityName} has no visual component.`);
      return null;
    }

    const visualDefinition = getRegistryEntry(this.registries, 'visuals', visualComponent.visual);
    if (!visualDefinition) {
      this.warnOnce(
        `visual:${visualComponent.visual}`,
        `Entity ${entityName} references missing visual ${visualComponent.visual}.`,
      );
      return null;
    }

    const texturePath = selectTexturePath(visualDefinition);
    const componentOffsetX = visualComponent.position?.offsetX ?? 0;
    const componentOffsetY = visualComponent.position?.offsetY ?? 0;
    const animations = resolveAnimations(visualDefinition, componentOffsetX, componentOffsetY);
    if (!texturePath && !animations) {
      this.warnOnce(`texture:${visualComponent.visual}`, `Visual ${visualComponent.visual} has no supported texture.`);
      return null;
    }

    return {
      texturePath: texturePath ?? undefined,
      animations,
      sortLayerOffset: visualDefinition.sortLayerOffset ?? 0,
      offsetX: (visualDefinition?.offsetX ?? 0) + componentOffsetX,
      offsetY: (visualDefinition?.offsetY ?? 0) + componentOffsetY,
      alpha: clampAlpha(visualComponent.alpha ?? 1),
      red: clampAlpha(visualComponent.red ?? 1),
      green: clampAlpha(visualComponent.green ?? 1),
      blue: clampAlpha(visualComponent.blue ?? 1),
      flipX: visualDefinition?.flipX ?? false,
      flipY: visualDefinition?.flipY ?? false,
      nameTag: resolveNameTag(this.registries, visualComponents),
    };
  }

  private warnOnce(key: string, message: string): void {
    if (this.warnedReasons.has(key)) {
      return;
    }

    this.warnedReasons.add(key);
    console.warn(`[Entities] ${message}`);
  }
}

function resolveNameTag(
  registries: ClientRegistrySnapshots,
  components: VisualComponentConfiguration[],
): ResolvedEntityNameTag | undefined {
  for (const component of components) {
    if (!component.visual) {
      continue;
    }
    const definition = getRegistryEntry(registries, 'visuals', component.visual);
    if (definition?.type !== 'text') {
      continue;
    }
    const text = component.overrides?.text ?? definition.text;
    if (typeof text !== 'string' || text.length === 0) {
      continue;
    }
    return {
      text,
      offsetX: component.position?.offsetX ?? 0,
      offsetY: component.position?.offsetY ?? 0,
    };
  }
  return undefined;
}

function parseComponentConfiguration(value: unknown): unknown {
  if (typeof value !== 'string') {
    return value;
  }
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}

function resolveAnimations(
  visual: ClientVisualDefinition,
  componentOffsetX: number,
  componentOffsetY: number,
): Record<string, ResolvedEntityAnimation> | undefined {
  const entries = Object.entries(visual.animations ?? {}).flatMap(([name, animation]) => {
    const textures = animation.textures?.filter((texture): texture is string => typeof texture === 'string') ?? [];
    if (textures.length === 0) {
      return [];
    }

    const frameDuration = typeof animation.duration === 'number' && animation.duration > 0
      ? animation.duration / textures.length
      : typeof animation.speed === 'number' && animation.speed > 0
        ? animation.speed
        : 1 / 30;
    return [[name, {
      textures,
      frameDuration,
      offsetX: (animation.offsetX ?? visual.offsetX ?? 0) + componentOffsetX,
      offsetY: (animation.offsetY ?? visual.offsetY ?? 0) + componentOffsetY,
      flipX: animation.flipX ?? false,
      flipY: animation.flipY ?? false,
    }] as const];
  });

  return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}

function isVisualComponent(value: unknown): value is VisualComponentConfiguration {
  return typeof value === 'object' && value !== null && (value as VisualComponentConfiguration).type === 'visual';
}

function clampAlpha(alpha: number): number {
  if (!Number.isFinite(alpha)) {
    return 1;
  }

  return Math.max(0, Math.min(1, alpha));
}

function selectTexturePath(visualDefinition: ClientVisualDefinition | undefined): string | null {
  if (!visualDefinition) {
    return null;
  }

  if (typeof visualDefinition.texture === 'string') {
    return visualDefinition.texture;
  }

  if (Array.isArray(visualDefinition.textures) && typeof visualDefinition.textures[0] === 'string') {
    return visualDefinition.textures[0];
  }

  const firstAnimation = Object.values(visualDefinition.animations ?? {})[0];
  return firstAnimation?.textures?.[0] ?? null;
}
