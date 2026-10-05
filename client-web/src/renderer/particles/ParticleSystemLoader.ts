import { Particle, ParticleContainer, type Texture } from 'pixi.js';
import type { ClientParticleSystemDefinition } from '@/data/ClientRegistrySchemas';

interface ActiveParticle {
  view: Particle;
  age: number;
  lifetime: number;
  directionX: number;
  directionY: number;
  speedMultiplier: number;
  scaleMultiplier: number;
}

/** PixiJS 8 runtime produced from Selene's portable particle subset. */
export class PixiParticleSystem {
  readonly container: ParticleContainer;
  private readonly particles: ActiveParticle[] = [];
  private elapsed = 0;
  private spawnTimer = 0;

  constructor(
    private readonly definition: ClientParticleSystemDefinition,
    private readonly texture: Texture,
    private readonly random: () => number = Math.random,
  ) {
    this.container = new ParticleContainer({
      texture,
      dynamicProperties: { position: true, rotation: false, vertex: true, color: true },
    });
    if (definition.additive) this.container.blendMode = 'add';
    this.spawnWave();
  }

  update(deltaSeconds: number): void {
    this.elapsed += deltaSeconds;
    this.spawnTimer += deltaSeconds;
    while (this.elapsed <= this.definition.emitterLifetime && this.spawnTimer >= this.definition.frequency) {
      this.spawnTimer -= this.definition.frequency;
      this.spawnWave();
    }
    for (let index = this.particles.length - 1; index >= 0; index -= 1) {
      const state = this.particles[index];
      state.age += deltaSeconds;
      if (state.age >= state.lifetime) {
        this.container.removeParticle(state.view);
        this.particles.splice(index, 1);
        continue;
      }
      const progress = state.age / state.lifetime;
      const speed = lerp(this.definition.speed.start, this.definition.speed.end, progress) * state.speedMultiplier;
      state.view.x += state.directionX * speed * deltaSeconds;
      state.view.y -= state.directionY * speed * deltaSeconds;
      const scale = lerp(this.definition.scale.start, this.definition.scale.end, progress) * state.scaleMultiplier;
      state.view.scaleX = scale;
      state.view.scaleY = scale;
      state.view.alpha = lerp(this.definition.alpha.start, this.definition.alpha.end, progress);
      state.view.tint = interpolateColor(this.definition.color.start, this.definition.color.end, progress);
    }
  }

  get complete(): boolean {
    return this.elapsed >= this.definition.emitterLifetime && this.particles.length === 0;
  }

  destroy(): void {
    this.particles.length = 0;
    this.container.removeFromParent();
    this.container.destroy();
  }

  private spawnWave(): void {
    const count = Math.min(
      this.definition.maxParticles - this.particles.length,
      1,
    );
    for (let index = 0; index < count; index += 1) this.spawnParticle();
  }

  private spawnParticle(): void {
    const angle = randomBetween(this.definition.rotation.min, this.definition.rotation.max, this.random);
    const radians = angle * Math.PI / 180;
    const scaleMultiplier = randomBetween(this.definition.scaleMinimumMultiplier, 1, this.random);
    const scale = this.definition.scale.start * scaleMultiplier;
    const position = spawnPosition(this.definition.spawn, this.random);
    const view = new Particle({
      texture: this.texture,
      x: position.x,
      y: position.y,
      anchorX: 0.5,
      anchorY: 0.5,
      rotation: radians,
      scaleX: scale,
      scaleY: scale,
      tint: this.definition.color.start,
      alpha: this.definition.alpha.start,
    });
    this.container.addParticle(view);
    this.particles.push({
      view,
      age: 0,
      lifetime: randomBetween(this.definition.lifetime.min, this.definition.lifetime.max, this.random),
      directionX: Math.cos(radians),
      directionY: Math.sin(radians),
      speedMultiplier: randomBetween(this.definition.speedMinimumMultiplier, 1, this.random),
      scaleMultiplier,
    });
  }
}

export function loadPixiParticleSystem(
  definition: ClientParticleSystemDefinition,
  texture: Texture,
): PixiParticleSystem {
  return new PixiParticleSystem(definition, texture);
}

function spawnPosition(
  spawn: ClientParticleSystemDefinition['spawn'],
  random: () => number,
): { x: number; y: number } {
  if (spawn.type === 'point') return { x: 0, y: 0 };
  return { x: (random() - 0.5) * spawn.width, y: (random() - 0.5) * spawn.height };
}

function randomBetween(minimum: number, maximum: number, random: () => number): number {
  return minimum + (maximum - minimum) * random();
}

function lerp(start: number, end: number, progress: number): number {
  return start + (end - start) * progress;
}

function interpolateColor(start: string, end: string, progress: number): number {
  const first = Number.parseInt(start.slice(1), 16);
  const last = Number.parseInt(end.slice(1), 16);
  const channel = (shift: number) => Math.round(lerp((first >> shift) & 0xff, (last >> shift) & 0xff, progress));
  return (channel(16) << 16) | (channel(8) << 8) | channel(0);
}
