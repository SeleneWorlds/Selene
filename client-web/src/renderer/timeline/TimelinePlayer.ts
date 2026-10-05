import { Container, Sprite, Texture } from 'pixi.js';
import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type {
  ClientTimelineDefinition,
  ClientVisualDefinition,
  ParticleSystemTimelineEvent,
  ScreenOverlayTimelineEvent,
  VisualAnimationTimelineEvent,
} from '@/data/ClientRegistrySchemas';
import type { Coordinate, PlayTimelinePacket, StopTimelinePacket } from '@/networking/GameProtocol';
import { ENTITY_LOCAL_SORT_LAYER, getRenderOrder, projectCoordinate } from '@/renderer/IsoProjection';
import type { ContentTextureLoader } from '@/renderer/entities/ContentTextureLoader';
import { loadPixiParticleSystem, type PixiParticleSystem } from '@/renderer/particles/ParticleSystemLoader';

interface Playback {
  instanceId: string;
  timeline: string;
  elapsedMs: number;
  nextEvent: number;
  events: ClientTimelineDefinition['events'];
  parameters: TimelineParameters;
}

interface TimelineParameters {
  values: Readonly<Record<string, unknown>>;
}

interface VisualEffect {
  instanceId: string;
  sprite: Sprite;
  textures: string[];
  elapsedMs: number;
  durationMs: number;
  frame: number;
  revision: number;
}

interface TileSurface {
  height: number;
  renderOrder: number | null;
}

interface ParticleEffect {
  instanceId: string;
  system: PixiParticleSystem;
  screenSpace: boolean;
}

interface OverlayEffect {
  instanceId: string;
  event: ScreenOverlayTimelineEvent;
  sprite: Sprite;
  elapsedMs: number;
}

/** Schedules typed timeline events and owns their temporary scene objects. */
export class TimelinePlayer {
  private readonly playbacks: Playback[] = [];
  private readonly effects: VisualEffect[] = [];
  private readonly particleEffects: ParticleEffect[] = [];
  private readonly overlayEffects: OverlayEffect[] = [];
  private readonly instanceTimelines = new Map<string, string>();
  private readonly instanceTags = new Map<string, ReadonlySet<string>>();
  private readonly pendingEffects = new Map<string, number>();
  private readonly stoppedInstances = new Set<string>();

  constructor(
    private readonly registries: ClientRegistrySnapshots,
    private readonly textureLoader: ContentTextureLoader,
    private readonly scene: Container,
    private readonly screenScene: Container,
    private readonly getSurface: (coordinate: Coordinate) => TileSurface,
    private readonly getViewport: () => { x: number; y: number; width: number; height: number },
  ) {}

  play(packet: PlayTimelinePacket): void {
    const timeline = getRegistryEntry(this.registries, 'timelines', packet.timeline);
    if (!timeline) {
      console.warn(`[Timelines] Unknown timeline ${packet.timeline}.`);
      return;
    }
    this.stoppedInstances.delete(packet.instanceId);
    this.instanceTimelines.set(packet.instanceId, packet.timeline);
    this.instanceTags.set(packet.instanceId, new Set(packet.tags));
    this.playbacks.push({
      instanceId: packet.instanceId,
      timeline: packet.timeline,
      elapsedMs: 0,
      nextEvent: 0,
      events: [...timeline.events].sort((left, right) => left.time - right.time),
      parameters: { values: packet.parameters },
    });
    this.updatePlaybacks(0);
  }

  stop(packet: StopTimelinePacket): void {
    const targets = packet.instanceId !== null
      ? [packet.instanceId]
      : packet.timeline !== null
        ? [...this.instanceTimelines].filter(([, timeline]) => timeline === packet.timeline).map(([id]) => id)
        : [...this.instanceTags].filter(([, tags]) => tags.has(packet.tag!)).map(([id]) => id);
    for (const instanceId of targets) {
      this.stoppedInstances.add(instanceId);
      for (let index = this.playbacks.length - 1; index >= 0; index -= 1) {
        if (this.playbacks[index].instanceId === instanceId) this.playbacks.splice(index, 1);
      }
      for (let index = this.effects.length - 1; index >= 0; index -= 1) {
        if (this.effects[index].instanceId !== instanceId) continue;
        this.effects[index].sprite.destroy();
        this.effects.splice(index, 1);
      }
      for (const effect of this.particleEffects) {
        if (effect.instanceId === instanceId) effect.system.stop();
      }
      for (let index = this.overlayEffects.length - 1; index >= 0; index -= 1) {
        if (this.overlayEffects[index].instanceId !== instanceId) continue;
        this.overlayEffects[index].sprite.destroy();
        this.overlayEffects.splice(index, 1);
      }
      this.cleanupInstance(instanceId);
    }
  }

  update(deltaMs: number, particlesEnabled = true): void {
    this.updatePlaybacks(deltaMs);
    const deltaSeconds = deltaMs / 1000;
    for (let index = this.particleEffects.length - 1; index >= 0; index -= 1) {
      const effect = this.particleEffects[index];
      effect.system.container.visible = particlesEnabled;
      if (!particlesEnabled) continue;
      if (effect.screenSpace) {
        const viewport = this.getViewport();
        effect.system.container.position.set(viewport.x + viewport.width / 2, viewport.y);
        effect.system.setScreenSpawnWidth(viewport.width);
      }
      effect.system.update(deltaSeconds);
      if (effect.system.complete) {
        effect.system.destroy();
        this.particleEffects.splice(index, 1);
        this.cleanupInstance(effect.instanceId);
      }
    }
    for (let index = this.effects.length - 1; index >= 0; index -= 1) {
      const effect = this.effects[index];
      effect.elapsedMs += deltaMs;
      if (effect.elapsedMs >= effect.durationMs) {
        effect.sprite.removeFromParent();
        effect.sprite.destroy();
        this.effects.splice(index, 1);
        this.cleanupInstance(effect.instanceId);
        continue;
      }
      const nextFrame = Math.min(
        effect.textures.length - 1,
        Math.floor(effect.elapsedMs / effect.durationMs * effect.textures.length),
      );
      if (nextFrame !== effect.frame) void this.setFrame(effect, nextFrame);
    }
    for (let index = this.overlayEffects.length - 1; index >= 0; index -= 1) {
      const effect = this.overlayEffects[index];
      effect.elapsedMs += deltaMs;
      if (effect.elapsedMs >= effect.event.duration * 1000) {
        effect.sprite.destroy();
        this.overlayEffects.splice(index, 1);
        this.cleanupInstance(effect.instanceId);
        continue;
      }
      this.updateOverlay(effect);
    }
  }

  private updatePlaybacks(deltaMs: number): void {
    for (let index = this.playbacks.length - 1; index >= 0; index -= 1) {
      const playback = this.playbacks[index];
      playback.elapsedMs += deltaMs;
      while (playback.nextEvent < playback.events.length
        && playback.events[playback.nextEvent].time * 1000 <= playback.elapsedMs) {
        this.execute(playback.events[playback.nextEvent], playback);
        playback.nextEvent += 1;
      }
      if (playback.nextEvent === playback.events.length) {
        this.playbacks.splice(index, 1);
        this.cleanupInstance(playback.instanceId);
      }
    }
  }

  private execute(event: ClientTimelineDefinition['events'][number], playback: Playback): void {
    switch (event.type) {
      case 'visual_animation':
        void this.playVisualAnimation(event, playback);
        break;
      case 'particle_system':
        void this.playParticleSystem(event, playback);
        break;
      case 'screen_overlay':
        this.playScreenOverlay(event, playback);
        break;
    }
  }

  private playScreenOverlay(event: ScreenOverlayTimelineEvent, playback: Playback): void {
    const sprite = new Sprite(Texture.WHITE);
    const effect = { instanceId: playback.instanceId, event, sprite, elapsedMs: 0 };
    this.overlayEffects.push(effect);
    this.screenScene.addChild(sprite);
    this.updateOverlay(effect);
  }

  private updateOverlay(effect: OverlayEffect): void {
    const viewport = this.getViewport();
    effect.sprite.position.set(viewport.x, viewport.y);
    effect.sprite.width = viewport.width;
    effect.sprite.height = viewport.height;
    effect.sprite.tint = keyedValue(effect.event, 'color', effect.event.color, effect.elapsedMs / 1000) as string;
    effect.sprite.alpha = Math.min(1, Math.max(0,
      keyedValue(effect.event, 'alpha', effect.event.alpha, effect.elapsedMs / 1000) as number,
    ));
  }

  private async playParticleSystem(
    event: ParticleSystemTimelineEvent,
    playback: Playback,
  ): Promise<void> {
    this.markPending(playback.instanceId, 1);
    const position = event.space === 'world' ? readCoordinate(playback.parameters.values[event.position]) : null;
    if (event.space === 'world' && !position) {
      console.warn(`[Timelines] Particle event ${event.particle} requires coordinate parameter ${event.position}.`);
      this.markPending(playback.instanceId, -1);
      return;
    }
    const definition = getRegistryEntry(this.registries, 'particles', event.particle);
    if (!definition) {
      console.warn(`[Timelines] Particle event references unknown system ${event.particle}.`);
      this.markPending(playback.instanceId, -1);
      return;
    }
    try {
      const texture = await this.textureLoader.load(definition.texture);
      if (this.stoppedInstances.has(playback.instanceId)) return;
      const initialViewport = event.space === 'screen' ? this.getViewport() : null;
      const emissionRateMultiplier = event.emissionRateMultiplier === undefined
        ? 1
        : readNonNegativeNumber(playback.parameters.values[event.emissionRateMultiplier], 1);
      const system = loadPixiParticleSystem(
        definition,
        texture,
        initialViewport?.width ?? null,
        emissionRateMultiplier,
      );
      const container = system.container;
      if (event.space === 'screen') {
        const viewport = this.getViewport();
        container.position.set(viewport.x + viewport.width / 2, viewport.y);
        system.setScreenSpawnWidth(viewport.width);
        this.screenScene.addChild(container);
      } else {
        const worldPosition = position!;
        const projected = projectCoordinate(worldPosition);
        const surface = this.getSurface(worldPosition);
        container.position.set(projected.x, projected.y - surface.height);
        const particleOrder = getRenderOrder(worldPosition, 0, ENTITY_LOCAL_SORT_LAYER);
        container.zIndex = surface.renderOrder === null
          ? particleOrder
          : Math.max(particleOrder, surface.renderOrder + ENTITY_LOCAL_SORT_LAYER);
        this.scene.addChild(container);
      }
      this.particleEffects.push({
        instanceId: playback.instanceId,
        system,
        screenSpace: event.space === 'screen',
      });
    } catch (error) {
      console.warn(`[Timelines] Failed to start particle system ${event.particle}.`, error);
    } finally {
      this.markPending(playback.instanceId, -1);
    }
  }

  private async playVisualAnimation(
    event: VisualAnimationTimelineEvent,
    playback: Playback,
  ): Promise<void> {
    const position = readCoordinate(playback.parameters.values[event.position]);
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
    const surface = this.getSurface(position);
    sprite.anchor.set(0.5, 1);
    sprite.position.set(
      projected.x + (visual.offsetX ?? 0),
      projected.y - surface.height - (visual.offsetY ?? 0),
    );
    sprite.scale.set(visual.flipX ? -1 : 1, visual.flipY ? -1 : 1);
    const visualOrder = getRenderOrder(position, visual.sortLayerOffset ?? 0, ENTITY_LOCAL_SORT_LAYER);
    sprite.zIndex = surface.renderOrder === null
      ? visualOrder
      : Math.max(visualOrder, surface.renderOrder + ENTITY_LOCAL_SORT_LAYER);
    const effect: VisualEffect = {
      instanceId: playback.instanceId, sprite, textures, elapsedMs: 0, durationMs, frame: 0, revision: 0,
    };
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

  private markPending(instanceId: string, delta: number): void {
    const count = (this.pendingEffects.get(instanceId) ?? 0) + delta;
    if (count > 0) this.pendingEffects.set(instanceId, count);
    else this.pendingEffects.delete(instanceId);
    this.cleanupInstance(instanceId);
  }

  private cleanupInstance(instanceId: string): void {
    if (this.playbacks.some(playback => playback.instanceId === instanceId)) return;
    if (this.effects.some(effect => effect.instanceId === instanceId)) return;
    if (this.particleEffects.some(effect => effect.instanceId === instanceId)) return;
    if (this.overlayEffects.some(effect => effect.instanceId === instanceId)) return;
    if (this.pendingEffects.has(instanceId)) return;
    this.instanceTimelines.delete(instanceId);
    this.instanceTags.delete(instanceId);
    this.stoppedInstances.delete(instanceId);
  }
}

function keyedValue(
  event: ScreenOverlayTimelineEvent,
  property: string,
  base: string | number,
  elapsed: number,
): string | number | boolean {
  const keys = [...(event.keys[property] ?? [])].sort((left, right) => left.time - right.time);
  let currentIndex = -1;
  for (let index = 0; index < keys.length && keys[index].time <= elapsed; index += 1) currentIndex = index;
  if (currentIndex < 0) return base;
  const current = keys[currentIndex];
  const next = keys[currentIndex + 1];
  if (!next || current.interpolation === 'step') return current.value;
  if (typeof current.value !== 'number' || typeof next.value !== 'number') return current.value;
  const progress = Math.min(1, Math.max(0, (elapsed - current.time) / (next.time - current.time)));
  return current.value + (next.value - current.value) * progress;
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

function readNonNegativeNumber(value: unknown, fallback: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return fallback;
  return Math.max(value, 0);
}
