import type { RegistryObjectApi } from './RegistriesApi';
export interface SoundsApi { playSound(sound: string | RegistryObjectApi, options?: { volume?: number; pitch?: number }): void; stopSound(sound: string | RegistryObjectApi): void; stopAllSounds(): void }
