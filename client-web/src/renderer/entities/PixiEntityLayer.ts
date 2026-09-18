import { Container, Graphics, Sprite, Text } from 'pixi.js';
import type { ClientEntitySnapshot } from '@/api/EntitiesApi';
import {
  ENTITY_LOCAL_SORT_LAYER,
  getSortLayer,
  projectCoordinate,
  ROW_SORT_SCALE,
} from '@/renderer/IsoProjection';
import type { Coordinate, EntityPacket, MoveEntityPacket } from '@/networking/GameProtocol';
import type { ContentTextureLoader } from './ContentTextureLoader';
import type { EntityVisualResolver } from './EntityVisualResolver';
import type { ResolvedEntityAnimation, ResolvedEntityVisual } from './EntityVisualResolver';
import type { ClientGrid } from '@/core/ClientGrid';

interface RenderedEntity {
  container: Container;
  fallback: Graphics;
  sprite: Sprite | null;
  nameTag: Text | null;
  texturePath: string | null;
  coordinate: Coordinate;
  facing: number;
  visual: ResolvedEntityVisual | null;
  motion: { start: Coordinate; end: Coordinate; durationMs: number; elapsedMs: number } | null;
  motionGraceRemainingMs: number;
  animationName: string | null;
  frameIndex: number;
  frameElapsedMs: number;
  visualRevision: number;
}

export interface WorldBounds {
  x: number;
  y: number;
  width: number;
  height: number;
}

export class PixiEntityLayer {
  private readonly renderedEntities = new Map<number, RenderedEntity>();
  private upperLayerFocusZ: number | null = null;
  private upperLayerAlpha: number | null = null;

  constructor(
    private readonly visualResolver: EntityVisualResolver,
    private readonly textureLoader: ContentTextureLoader,
    private readonly grid: ClientGrid,
    readonly container: Container,
  ) {}

  update(deltaMs: number): void {
    for (const [id, rendered] of this.renderedEntities) {
      if (rendered.motion) {
        rendered.motion.elapsedMs += deltaMs;
        const progress = Math.min(1, rendered.motion.elapsedMs / rendered.motion.durationMs);
        rendered.coordinate = interpolateCoordinate(rendered.motion.start, rendered.motion.end, progress);
        this.positionEntity(rendered);
        if (progress >= 1) {
          rendered.coordinate = { ...rendered.motion.end };
          rendered.motion = null;
          rendered.motionGraceRemainingMs = 50;
        }
      } else {
        rendered.motionGraceRemainingMs = Math.max(0, rendered.motionGraceRemainingMs - deltaMs);
      }
      this.updateAnimation(id, rendered, deltaMs);
    }
  }

  upsertServerEntity(packet: EntityPacket): void {
    void this.upsertEntity(packet);
  }

  moveServerEntity(packet: MoveEntityPacket): void {
    this.moveEntity(packet);
  }

  turnServerEntity(networkId: number, facing: number): void {
    this.turnEntity(networkId, facing);
  }

  removeServerEntity(networkId: number): void {
    this.removeEntity(networkId);
  }

  getEntityCoordinate(networkId: number): Coordinate | null {
    return this.renderedEntities.get(networkId)?.coordinate ?? null;
  }

  getEntityBounds(networkId: number): WorldBounds | null {
    const rendered = this.renderedEntities.get(networkId);
    if (!rendered) {
      return null;
    }

    if (rendered.sprite) {
      const sprite = rendered.sprite;
      const left = sprite.scale.x >= 0
        ? -sprite.width * sprite.anchor.x
        : -sprite.width * (1 - sprite.anchor.x);
      const top = sprite.scale.y >= 0
        ? -sprite.height * sprite.anchor.y
        : -sprite.height * (1 - sprite.anchor.y);
      return {
        x: rendered.container.x + sprite.x + left,
        y: rendered.container.y + sprite.y + top,
        width: sprite.width,
        height: sprite.height,
      };
    }

    const bounds = rendered.fallback.getLocalBounds();
    return {
      x: rendered.container.x + rendered.fallback.x + bounds.x,
      y: rendered.container.y + rendered.fallback.y + bounds.y,
      width: bounds.width,
      height: bounds.height,
    };
  }

  setUpperLayerAlpha(focusZ: number, alpha: number): void {
    if (this.upperLayerFocusZ === focusZ && this.upperLayerAlpha === alpha) {
      return;
    }
    this.upperLayerFocusZ = focusZ;
    this.upperLayerAlpha = alpha;
    for (const entity of this.renderedEntities.values()) {
      entity.container.alpha = entity.coordinate.z > focusZ ? alpha : 1;
    }
  }

  upsertClientEntity(snapshot: ClientEntitySnapshot): void {
    const rendered = this.getOrCreateEntity(snapshot.id);
    rendered.coordinate = snapshot.coordinate;
    rendered.facing = snapshot.facing;
    rendered.motion = null;
    rendered.motionGraceRemainingMs = 0;
    this.positionEntity(rendered);

    const visual = this.visualResolver.resolveDefinition(snapshot.entityName, snapshot.definition, snapshot.components);
    if (!visual) {
      showFallback(rendered);
      return;
    }

    void this.applyVisual(snapshot.id, rendered, visual);
  }

  setClientEntityAlpha(id: number, alpha: number): void {
    const rendered = this.renderedEntities.get(id);
    if (!rendered?.visual) {
      return;
    }
    rendered.visual.alpha = alpha;
    if (rendered.sprite) {
      rendered.sprite.alpha = alpha;
    }
  }

  removeClientEntity(id: number): void {
    this.removeEntity(id);
  }

  private async upsertEntity(packet: EntityPacket): Promise<void> {
    const rendered = this.getOrCreateEntity(packet.networkId);
    rendered.coordinate = packet.coordinate;
    rendered.facing = packet.facing;
    rendered.motion = null;
    rendered.motionGraceRemainingMs = 0;
    this.positionEntity(rendered);

    const visual = this.visualResolver.resolve(packet);
    if (!visual) {
      showFallback(rendered);
      return;
    }

    await this.applyVisual(packet.networkId, rendered, visual);
  }

  private moveEntity(packet: MoveEntityPacket): void {
    const rendered = this.renderedEntities.get(packet.networkId);
    if (!rendered) {
      return;
    }

    rendered.facing = packet.facing;
    rendered.motion = {
      start: { ...packet.start },
      end: { ...packet.end },
      durationMs: Math.max(packet.duration * 1000, 1),
      elapsedMs: 0,
    };
    rendered.motionGraceRemainingMs = 50;
    rendered.coordinate = { ...packet.start };
    this.positionEntity(rendered);
  }

  private turnEntity(networkId: number, facing: number): void {
    const rendered = this.renderedEntities.get(networkId);
    if (rendered) {
      rendered.facing = facing;
      rendered.animationName = null;
    }
  }

  private removeEntity(networkId: number): void {
    const rendered = this.renderedEntities.get(networkId);
    if (!rendered) {
      return;
    }

    rendered.container.destroy({ children: true });
    this.renderedEntities.delete(networkId);
  }

  private getOrCreateEntity(networkId: number): RenderedEntity {
    const existing = this.renderedEntities.get(networkId);
    if (existing) {
      return existing;
    }

    const container = new Container();
    const fallback = createFallbackMarker();
    container.addChild(fallback);
    this.container.addChild(container);

    const rendered: RenderedEntity = {
      container,
      fallback,
      sprite: null,
      nameTag: null,
      texturePath: null,
      coordinate: { x: 0, y: 0, z: 0 },
      facing: 0,
      visual: null,
      motion: null,
      motionGraceRemainingMs: 0,
      animationName: null,
      frameIndex: 0,
      frameElapsedMs: 0,
      visualRevision: 0,
    };
    this.renderedEntities.set(networkId, rendered);
    return rendered;
  }

  private positionEntity(entity: RenderedEntity): void {
    const position = projectCoordinate(entity.coordinate);
    entity.container.position.set(position.x, position.y);
    // Tiles occupy discrete sort rows. A continuously interpolated zIndex made
    // Pixi re-sort and rebuild the tile-heavy render list every movement frame,
    // even though ordering can only change when the entity crosses a row.
    const rowSortLayer = Math.round(getSortLayer(entity.coordinate) / ROW_SORT_SCALE) * ROW_SORT_SCALE;
    const zIndex = -(
      rowSortLayer - (entity.visual?.sortLayerOffset ?? 0)
    ) * 1000 + ENTITY_LOCAL_SORT_LAYER;
    if (entity.container.zIndex !== zIndex) {
      entity.container.zIndex = zIndex;
    }
    if (this.upperLayerFocusZ !== null && this.upperLayerAlpha !== null) {
      const alpha = entity.coordinate.z > this.upperLayerFocusZ ? this.upperLayerAlpha : 1;
      if (entity.container.alpha !== alpha) {
        entity.container.alpha = alpha;
      }
    }
  }

  private async applyVisual(
    id: number,
    rendered: RenderedEntity,
    visual: NonNullable<ReturnType<EntityVisualResolver['resolveDefinition']>>,
  ): Promise<void> {
    if (visual.animations) {
      const texturePaths = new Set(
        Object.values(visual.animations).flatMap((animation) => animation.textures),
      );
      try {
        await Promise.all([...texturePaths].map((texturePath) => this.textureLoader.load(texturePath)));
      } catch (error) {
        console.warn('Failed to preload entity animation textures.', error);
        showFallback(rendered);
        return;
      }
      if (this.renderedEntities.get(id) !== rendered) {
        return;
      }
    }

    rendered.visual = visual;
    applyNameTag(rendered);
    this.positionEntity(rendered);
    rendered.animationName = null;
    rendered.fallback.visible = false;

    if (visual.animations) {
      this.updateAnimation(id, rendered, 0);
      return;
    }

    if (!visual.texturePath) {
      showFallback(rendered);
      return;
    }

    if (rendered.sprite && rendered.texturePath === visual.texturePath) {
      applySpriteVisual(rendered.sprite, visual);
      positionNameTag(rendered);
      return;
    }

    try {
      const texture = await this.textureLoader.load(visual.texturePath);
      if (!this.renderedEntities.has(id)) {
        return;
      }

      rendered.sprite?.destroy();
      rendered.sprite = new Sprite(texture);
      rendered.texturePath = visual.texturePath;
      applySpriteVisual(rendered.sprite, visual);
      rendered.container.addChild(rendered.sprite);
      positionNameTag(rendered);
    } catch (error) {
      console.warn(`Failed to load entity texture ${visual.texturePath}.`, error);
      showFallback(rendered);
    }
  }

  private updateAnimation(id: number, rendered: RenderedEntity, deltaMs: number): void {
    const animations = rendered.visual?.animations;
    if (!animations) {
      return;
    }

    const direction = this.grid.getDirection(rendered.facing)?.name ?? 'south';
    const state = rendered.motion || rendered.motionGraceRemainingMs > 0 ? 'walk' : 'stationary';
    const requestedName = `${state}/${direction}`;
    const animationName = animations[requestedName]
      ? requestedName
      : Object.keys(animations).find((name) => name.startsWith(`${state}/`)) ?? Object.keys(animations)[0];
    if (!animationName) {
      return;
    }

    const animation = animations[animationName];
    if (rendered.animationName !== animationName) {
      rendered.animationName = animationName;
      rendered.frameIndex = 0;
      rendered.frameElapsedMs = 0;
      void this.applyAnimationFrame(id, rendered, animation);
      return;
    }

    rendered.frameElapsedMs += deltaMs;
    const frameDurationMs = animation.frameDuration * 1000;
    const nextFrame = Math.floor(rendered.frameElapsedMs / frameDurationMs) % animation.textures.length;
    if (nextFrame !== rendered.frameIndex) {
      rendered.frameIndex = nextFrame;
      void this.applyAnimationFrame(id, rendered, animation);
    }
  }

  private async applyAnimationFrame(id: number, rendered: RenderedEntity, animation: ResolvedEntityAnimation): Promise<void> {
    const texturePath = animation.textures[rendered.frameIndex];
    const revision = ++rendered.visualRevision;
    try {
      const texture = await this.textureLoader.load(texturePath);
      if (this.renderedEntities.get(id) !== rendered || revision !== rendered.visualRevision) {
        return;
      }
      if (!rendered.sprite) {
        rendered.sprite = new Sprite(texture);
        rendered.container.addChild(rendered.sprite);
      } else {
        rendered.sprite.texture = texture;
      }
      rendered.texturePath = texturePath;
      applySpriteVisual(rendered.sprite, { ...rendered.visual!, ...animation, texturePath });
      positionNameTag(rendered);
    } catch (error) {
      console.warn(`Failed to load entity animation texture ${texturePath}.`, error);
    }
  }
}

function interpolateCoordinate(start: Coordinate, end: Coordinate, progress: number): Coordinate {
  return {
    x: start.x + (end.x - start.x) * progress,
    y: start.y + (end.y - start.y) * progress,
    z: start.z + (end.z - start.z) * progress,
  };
}

function showFallback(rendered: RenderedEntity): void {
  rendered.sprite?.destroy();
  rendered.sprite = null;
  rendered.texturePath = null;
  rendered.fallback.visible = true;
  positionNameTag(rendered);
}

function applyNameTag(rendered: RenderedEntity): void {
  const resolved = rendered.visual?.nameTag;
  if (!resolved) {
    rendered.nameTag?.destroy();
    rendered.nameTag = null;
    return;
  }

  if (!rendered.nameTag) {
    rendered.container.sortableChildren = true;
    rendered.nameTag = new Text({
      text: resolved.text,
      style: {
        fill: 0xffffff,
        fontFamily: 'Arial, sans-serif',
        fontSize: 14,
        stroke: { color: 0x000000, width: 3 },
      },
    });
    rendered.nameTag.anchor.set(0.5, 1);
    rendered.nameTag.zIndex = 1;
    rendered.container.addChild(rendered.nameTag);
  } else {
    rendered.nameTag.text = resolved.text;
  }
  positionNameTag(rendered);
}

function positionNameTag(rendered: RenderedEntity): void {
  const nameTag = rendered.nameTag;
  const resolved = rendered.visual?.nameTag;
  if (!nameTag || !resolved) {
    return;
  }
  const visualTop = rendered.sprite
    ? rendered.sprite.position.y - rendered.sprite.height
    : rendered.fallback.getLocalBounds().y;
  nameTag.position.set(resolved.offsetX, visualTop - resolved.offsetY);
}

function applySpriteVisual(
  sprite: Sprite,
  visual: NonNullable<ReturnType<EntityVisualResolver['resolveDefinition']>>,
): void {
  sprite.alpha = visual.alpha;
  sprite.tint = (Math.round(visual.red * 255) << 16)
    | (Math.round(visual.green * 255) << 8)
    | Math.round(visual.blue * 255);
  sprite.anchor.set(0.5, 1);
  sprite.position.set(visual.offsetX, -visual.offsetY);
  sprite.scale.x = visual.flipX ? -1 : 1;
  sprite.scale.y = visual.flipY ? -1 : 1;
}

function createFallbackMarker(): Graphics {
  const fallback = new Graphics()
    .moveTo(0, -38)
    .lineTo(36, 0)
    .lineTo(0, 38)
    .lineTo(-36, 0)
    .closePath()
    .fill({ color: 0xd5c06f, alpha: 0.85 })
    .stroke({ color: 0xffffff, alpha: 0.55, width: 2 });
  fallback.visible = false;
  return fallback;
}
