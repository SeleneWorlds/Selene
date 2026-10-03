import type { RegistryObjectApi, RegistriesApi } from '@/api/RegistriesApi';
import type { SoundsApi } from '@/api/SoundsApi';
import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import type { ClientAssetManifest } from './ClientAssetManifest';
import type { GamePacket } from '@/networking/GameProtocol';
import { clamp, numberOr } from './utils';
import { getModAsset } from './ModAssetStore';

export class SoundsService implements SoundsApi {
  private readonly playing = new Map<string, Set<HTMLAudioElement>>();
  private readonly pending = new Map<string, Set<AbortController>>();
  private readonly objectUrls = new Map<HTMLAudioElement, string>();
  constructor(private readonly registries: RegistriesApi, private readonly manifest: ClientAssetManifest, private readonly serverUrl: string, private readonly authToken: string) {}
  readonly handlePacket = (packet: GamePacket) => {
    if (packet.type === 'playSound') {
      const sound = this.findSoundById(packet.soundId);
      if (!sound) { console.warn(`Could not play sound with id ${packet.soundId}`); return; }
      this.playSound(sound, { volume: packet.volume, pitch: packet.pitch });
    } else if (packet.type === 'stopSound') {
      if (packet.soundId === -1) { this.stopAllSounds(); return; }
      const sound = this.findSoundById(packet.soundId);
      if (!sound) { console.warn(`Could not stop sound with id ${packet.soundId}`); return; }
      this.stopSound(sound);
    }
  };
  playSound(value: string | RegistryObjectApi, options: { volume?: number; pitch?: number } = {}) {
    const [name, sound] = this.resolve(value); const file = requireString(sound.file, 'sound file').replace(/^\/+/, '');
    const controller = new AbortController(); let pending = this.pending.get(name);
    if (!pending) { pending = new Set(); this.pending.set(name, pending); } pending.add(controller);
    void getModAsset(file).then(async (modAsset) => {
      if (controller.signal.aborted) throw new DOMException('The request was aborted.', 'AbortError');
      if (modAsset) return modAsset;
      const source = resolveServerUrl(this.serverUrl, this.manifest.assets[file] ?? `/client/content/${file}`);
      const response = await fetch(source, { headers: { Authorization: `Bearer ${this.authToken}` }, signal: controller.signal });
      if (!response.ok) throw new Error(`${response.status} ${response.statusText}`);
      return response.blob();
    })
      .then(async (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        const audio = new Audio(objectUrl); this.objectUrls.set(audio, objectUrl);
        audio.volume = clamp(options.volume ?? numberOr(sound.volume, 1), 0, 1); audio.playbackRate = clamp(options.pitch ?? numberOr(sound.pitch, 1), .25, 4);
        audio.loop = sound.loop === true || sound.type === 'music'; let set = this.playing.get(name);
        if (!set) { set = new Set(); this.playing.set(name, set); } set.add(audio);
        audio.addEventListener('ended', () => this.releaseAudio(name, audio), { once: true });
        await audio.play();
      })
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) console.error(`[Lua] Failed to play sound ${name}`, error);
      })
      .finally(() => { pending?.delete(controller); if (pending?.size === 0) this.pending.delete(name); });
  }
  stopSound(value: string | RegistryObjectApi) { const [name] = this.resolve(value); for (const request of this.pending.get(name) ?? []) request.abort(); this.pending.delete(name); for (const audio of this.playing.get(name) ?? []) { audio.pause(); audio.currentTime = 0; this.releaseAudio(name, audio); } this.playing.delete(name); }
  stopAllSounds() { for (const name of new Set([...this.playing.keys(), ...this.pending.keys()])) { const sound = this.registries.findByName('sounds', name); if (sound) this.stopSound(sound); else { for (const request of this.pending.get(name) ?? []) request.abort(); this.pending.delete(name); for (const audio of this.playing.get(name) ?? []) { audio.pause(); this.releaseAudio(name, audio); } this.playing.delete(name); } } }
  private releaseAudio(name: string, audio: HTMLAudioElement) { this.playing.get(name)?.delete(audio); const objectUrl = this.objectUrls.get(audio); if (objectUrl) URL.revokeObjectURL(objectUrl); this.objectUrls.delete(audio); }
  private findSoundById(id: number) { return this.registries.findAll('sounds').find((sound) => sound.getId() === id) ?? null; }
  private resolve(value: string | RegistryObjectApi): [string, RegistryObjectApi] {
    const sound = typeof value === 'string' ? this.registries.findByName('sounds', value) : value;
    if (!sound) throw new Error(`Unknown sound: ${String(value)}`); const name = sound.getName();
    if (typeof sound.file === 'string') return [name, sound];
    if (typeof sound.audio !== 'string') throw new Error('Sound definition has no audio reference.');
    const audio = this.registries.findByName('audio', sound.audio); if (!audio) throw new Error(`Unknown audio definition: ${sound.audio}`); return [name, audio];
  }
}

function requireString(value: unknown, label: string): string { if (typeof value !== 'string') throw new Error(`Expected ${label} to be a string.`); return value; }
