import type { RegistriesApi } from '@/api/RegistriesApi';
import type { VisualApi, VisualsApi } from '@/api/VisualsApi';
import { numberOr } from './utils';

export class VisualsService implements VisualsApi {
  constructor(private readonly registries: RegistriesApi) {}
  create(identifier: string): VisualApi {
    const definition = this.registries.findByName('visuals', identifier);
    if (!definition) throw new Error(`Unknown visual: ${identifier}`);
    const drawable = { getTextureRegion: () => null, getCurrentFrame: () => 0, getElapsedTime: () => 0,
      getDuration: () => numberOr(definition.duration, 0), animationCompleted: { connect: () => undefined }, withoutOffset() { return this; } };
    return Object.assign({}, definition, {
      getMetadata: (key: string) => definition.getMetadata(key), getSurfaceHeight: () => numberOr(definition.surfaceOffsetY, 0),
      getDrawable: () => drawable, getDefinition: () => definition,
    });
  }
}
