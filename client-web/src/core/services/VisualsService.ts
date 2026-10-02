import type { RegistriesApi } from '@/api/RegistriesApi';
import type { VisualApi, VisualsApi } from '@/api/VisualsApi';
import { numberOr } from './utils';

export class VisualsService implements VisualsApi {
  constructor(private readonly registries: RegistriesApi) {}
  create(identifier: string): VisualApi {
    const definition = this.registries.findByName('visuals', identifier);
    if (!definition) throw new Error(`Unknown visual: ${identifier}`);
    const duration = numberOr(definition.duration, 0);
    const animationCompleted = {
      connect: (callback: (...args: unknown[]) => void) => {
        const timeout = window.setTimeout(callback, Math.max(0, duration * 1000));
        return () => window.clearTimeout(timeout);
      },
    };
    const drawable = { getTextureRegion: () => null, getCurrentFrame: () => 0, getElapsedTime: () => 0,
      getDuration: () => duration,
      getAnimationCompleted: () => animationCompleted,
      animationCompleted,
      withoutOffset() { return this; } };
    return Object.assign({}, definition, {
      getMetadata: (key: string) => definition.getMetadata(key), getSurfaceHeight: () => numberOr(definition.surfaceOffsetY, 0),
      getDrawable: () => drawable, getDefinition: () => definition,
    });
  }
}
