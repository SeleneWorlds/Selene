import type { RegistryObjectApi, RegistriesApi } from '@/api/RegistriesApi';
import type { SoundsApi } from '@/api/SoundsApi';
import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import type { ClientAssetManifest } from './ClientAssetManifest';
import { clamp, numberOr } from './utils';

export class SoundsService implements SoundsApi {
  private readonly playing = new Map<string, Set<HTMLAudioElement>>();
  constructor(private readonly registries: RegistriesApi, private readonly manifest: ClientAssetManifest, private readonly serverUrl: string) {}
  playSound(value: string | RegistryObjectApi, options: { volume?: number; pitch?: number } = {}) {
    const [name, sound] = this.resolve(value); const file = requireString(sound.file, 'sound file').replace(/^\/+/, '');
    const audio = new Audio(resolveServerUrl(this.serverUrl, this.manifest.assets[file] ?? `/client/content/${file}`));
    audio.volume = clamp(options.volume ?? numberOr(sound.volume, 1), 0, 1); audio.playbackRate = clamp(options.pitch ?? numberOr(sound.pitch, 1), .25, 4);
    audio.loop = sound.loop === true || sound.type === 'music'; let set = this.playing.get(name);
    if (!set) { set = new Set(); this.playing.set(name, set); } set.add(audio);
    audio.addEventListener('ended', () => set?.delete(audio), { once: true }); void audio.play().catch((e) => console.error(`[Lua] Failed to play sound ${name}`, e));
  }
  stopSound(value: string | RegistryObjectApi) { const [name] = this.resolve(value); for (const audio of this.playing.get(name) ?? []) { audio.pause(); audio.currentTime = 0; } this.playing.delete(name); }
  stopAllSounds() { for (const name of [...this.playing.keys()]) { const sound = this.registries.findByName('sounds', name); if (sound) this.stopSound(sound); else { for (const audio of this.playing.get(name) ?? []) audio.pause(); this.playing.delete(name); } } }
  private resolve(value: string | RegistryObjectApi): [string, RegistryObjectApi] {
    const sound = typeof value === 'string' ? this.registries.findByName('sounds', value) : value;
    if (!sound) throw new Error(`Unknown sound: ${String(value)}`); const name = sound.getName();
    if (typeof sound.file === 'string') return [name, sound];
    if (typeof sound.audio !== 'string') throw new Error('Sound definition has no audio reference.');
    const audio = this.registries.findByName('audio', sound.audio); if (!audio) throw new Error(`Unknown audio definition: ${sound.audio}`); return [name, audio];
  }
}

function requireString(value: unknown, label: string): string { if (typeof value !== 'string') throw new Error(`Expected ${label} to be a string.`); return value; }
