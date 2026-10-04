import { Sprite, type Container } from 'pixi.js';
import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type {
  ClientTimelineDefinition,
  ClientVisualDefinition,
  VisualAnimationTimelineEvent,
} from '@/data/ClientRegistrySchemas';
import type { Coordinate, PlayTimelinePacket } from '@/networking/GameProtocol';
import { getRenderOrder, projectCoordinate } from '@/renderer/IsoProjection';
import type { ContentTextureLoader } from '@/renderer/entities/ContentTextureLoader';

interface Playback {
  elapsedMs: number;
  nextEvent: number;
  events: ClientTimelineDefinition['events'];
  parameters: TimelineParameters;
}

interface TimelineParameters {
  values: Readonly<Record<string, unknown>>;
}

interface VisualEffect {
  sprite: Sprite;
  textures: string[];
  elapsedMs: number;
  durationMs: number;
  frame: number;
  revision: number;
}

/** Schedules typed timeline events and owns their temporary scene objects. */
export class TimelinePlayer {
  private readonly playbacks: Playback[] = [];
  private readonly effects: VisualEffect[] = [];

  constructor(
    private readonly registries: ClientRegistrySnapshots,
    private readonly textureLoader: ContentTextureLoader,
    private readonly scene: Container,
  ) {}

  play(packet: PlayTimelinePacket): void {
    const timeline = getRegistryEntry(this.registries, 'timelines', packet.timeline);
    if (!timeline) {
      console.warn(`[Timelines] Unknown timeline ${packet.timeline}.`);
      return;
    }
    this.playbacks.push({
      elapsedMs: 0,
      nextEvent: 0,
      events: [...timeline.events].sort((left, right) => left.time - right.time),
      parameters: { values: packet.parameters },
    });
    this.updatePlaybacks(0);
  }

  update(deltaMs: number): void {
    this.updatePlaybacks(deltaMs);
    for (let index = this.effects.length - 1; index >= 0; index -= 1) {
      const effect = this.effects[index];
      effect.elapsedMs += deltaMs;
      if (effect.elapsedMs >= effect.durationMs) {
        effect.sprite.removeFromParent();
        effect.sprite.destroy();
        this.effects.splice(index, 1);
        continue;
      }
      const nextFrame = Math.min(
        effect.textures.length - 1,
        Math.floor(effect.elapsedMs / effect.durationMs * effect.textures.length),
      );
      if (nextFrame !== effect.frame) void this.setFrame(effect, nextFrame);
    }
  }

  private updatePlaybacks(deltaMs: number): void {
    for (let index = this.playbacks.length - 1; index >= 0; index -= 1) {
      const playback = this.playbacks[index];
      playback.elapsedMs += deltaMs;
      while (playback.nextEvent < playback.events.length
        && playback.events[playback.nextEvent].time * 1000 <= playback.elapsedMs) {
        this.execute(playback.events[playback.nextEvent], playback.parameters);
        playback.nextEvent += 1;
      }
      if (playback.nextEvent === playback.events.length) this.playbacks.splice(index, 1);
    }
  }

  private execute(event: ClientTimelineDefinition['events'][number], parameters: TimelineParameters): void {
    switch (event.type) {
      case 'visual_animation':
        void this.playVisualAnimation(event, parameters);
        break;
    }
  }

  private async playVisualAnimation(
    event: VisualAnimationTimelineEvent,
    parameters: TimelineParameters,
  ): Promise<void> {
    const position = readCoordinate(parameters.values[event.position]);
    if (!position) {
      console.warn(`[Timelines] Visual event ${event.visual} requires coordinate parameter ${event.position}.`);
      return;
    }
    const visual = getRegistryEntry(this.registries, 'visuals', event.visual);
    const textures = getAnimationTextures(visual);
    if (!visual || textures.length === 0) {
      console.warn(`[Timelines] Visual event references unsupported visual ${event.visual}.`);
      return;
    }
    const durationMs = (event.duration ?? visual.duration ?? 1) * 1000;
    const sprite = new Sprite();
    const projected = projectCoordinate(position);
    sprite.position.set(projected.x + (visual.offsetX ?? 0), projected.y - (visual.offsetY ?? 0));
    sprite.scale.set(visual.flipX ? -1 : 1, visual.flipY ? -1 : 1);
    sprite.zIndex = getRenderOrder(position, visual.sortLayerOffset ?? 0, 0.75);
    const effect: VisualEffect = { sprite, textures, elapsedMs: 0, durationMs, frame: 0, revision: 0 };
    this.effects.push(effect);
    this.scene.addChild(sprite);
    await this.setFrame(effect, 0);
  }

  private async setFrame(effect: VisualEffect, frame: number): Promise<void> {
    effect.frame = frame;
    const revision = ++effect.revision;
    try {
      const texture = await this.textureLoader.load(effect.textures[frame]);
      if (revision === effect.revision && !effect.sprite.destroyed) effect.sprite.texture = texture;
    } catch (error) {
      console.warn(`[Timelines] Failed to load visual frame ${effect.textures[frame]}.`, error);
    }
  }
}

function getAnimationTextures(visual: ClientVisualDefinition | undefined): string[] {
  if (!visual) return [];
  if (visual.type === 'animated') return visual.textures ?? [];
  if (visual.type === 'simple' && visual.texture) return [visual.texture];
  return [];
}

function readCoordinate(value: unknown): Coordinate | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null;
  const coordinate = value as Record<string, unknown>;
  return Number.isInteger(coordinate.x) && Number.isInteger(coordinate.y) && Number.isInteger(coordinate.z)
    ? { x: coordinate.x as number, y: coordinate.y as number, z: coordinate.z as number }
    : null;
}
