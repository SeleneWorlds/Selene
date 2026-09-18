import type { RegistryObjectApi } from './RegistriesApi';
import type { SignalApi } from './EventsApi';
export interface DrawableApi { getTextureRegion(): unknown; getCurrentFrame(): number; getElapsedTime(): number; getDuration(): number; animationCompleted: SignalApi; withoutOffset(): DrawableApi }
export interface VisualApi extends Record<string, unknown> { getMetadata(key: string): unknown; getSurfaceHeight(): number; getDrawable(): DrawableApi; getDefinition(): RegistryObjectApi }
export interface VisualsApi { create(identifier: string): VisualApi }
