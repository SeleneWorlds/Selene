import type { MapApi } from '@/api/MapApi';
import type { ResourcesApi } from '@/api/ResourcesApi';
import type { SoundsApi } from '@/api/SoundsApi';
import type { TexturesApi } from '@/api/TexturesApi';
import type { VisualsApi } from '@/api/VisualsApi';
import { LuaMultiReturn } from 'wasmoon';
import type { LuaRuntime } from './LuaRuntime';
import { LuaArguments } from './LuaArguments';
import { toLuaCoordinate } from './LuaObjects';
import { createSignal } from './LuaSignal';

export interface ClientFeatureLuaApis { map: MapApi; sounds: SoundsApi; resources: ResourcesApi; textures: TexturesApi; visuals: VisualsApi }

export async function registerClientFeatureLuaModules(runtime: LuaRuntime, apis: ClientFeatureLuaApis): Promise<void> {
  const changed = createSignal('selene.map.onChunkChanged', (callback) => apis.map.addChunkChangedListener((coordinate, width, height) => callback(toLuaCoordinate(coordinate), width, height)));
  await runtime.preloadModule('selene.map', () => ({
    getTilesAt: (value: unknown) => apis.map.getTilesAt(coordinate('map.getTilesAt', value)),
    hasTileAt: (value: unknown) => apis.map.hasTileAt(coordinate('map.hasTileAt', value)), onChunkChanged: changed,
  }));
  const play = (sound: unknown, options?: unknown) => apis.sounds.playSound(sound as Parameters<SoundsApi['playSound']>[0], record(options));
  await runtime.preloadModule('selene.sounds', () => ({ playSound: play, playLocalSound: play, stopSound: (sound: unknown) => apis.sounds.stopSound(sound as Parameters<SoundsApi['stopSound']>[0]), stopAllSounds: () => apis.sounds.stopAllSounds() }));
  await runtime.preloadModule('selene.resources', () => ({
    listFiles: (bundle: unknown, filter: unknown) => apis.resources.listFiles(str('resources.listFiles', bundle, 'bundle'), str('resources.listFiles', filter, 'filter')),
    loadAsString: (path: unknown) => apis.resources.loadAsString(str('resources.loadAsString', path, 'path')),
    fileExists: (path: unknown) => apis.resources.fileExists(str('resources.fileExists', path, 'path')),
  }));
  await runtime.preloadModule('selene.textures', () => ({ create: (width: unknown, height: unknown, format?: unknown) => textureObject(apis.textures.create(int(width, 'width'), int(height, 'height'), format == null ? undefined : String(format))) }));
  const createVisual = (id: unknown) => apis.visuals.create(str('visuals.create', id, 'identifier'));
  await runtime.preloadModule('selene.visuals.internal', () => ({ create: createVisual }));
  await runtime.preloadModule('selene.visuals', () => ({ create: createVisual }));
}

function textureObject(api: ReturnType<TexturesApi['create']>): Record<string, unknown> {
  return { getWidth: () => api.getWidth(), getHeight: () => api.getHeight(), setPixel: (x: unknown, y: unknown, color: unknown) => api.setPixel(int(x, 'x'), int(y, 'y'), color),
    getPixel: (x: unknown, y: unknown) => LuaMultiReturn.of(...api.getPixel(int(x, 'x'), int(y, 'y'))), fill: (color: unknown) => api.fill(color),
    copyFrom: (source: unknown, sx: unknown, sy: unknown, width: unknown, height: unknown, dx: unknown, dy: unknown) => api.copyFrom(textureApi(source), int(sx, 'srcX'), int(sy, 'srcY'), int(width, 'width'), int(height, 'height'), int(dx, 'dstX'), int(dy, 'dstY')),
    update: () => api.update(), dispose: () => api.dispose(), __api: api };
}
function textureApi(value: unknown) { const valueRecord = record(value); if (!valueRecord?.__api) throw new Error('Expected a scriptable texture.'); return valueRecord.__api as ReturnType<TexturesApi['create']>; }
function coordinate(context: string, value: unknown) { return new LuaArguments(`selene.${context}`).coordinate(value, 'coordinate'); }
function str(context: string, value: unknown, name: string) { return new LuaArguments(`selene.${context}`).string(value, name); }
function int(value: unknown, name: string) { return new LuaArguments('selene.textures').integer(value, name); }
function record(value: unknown): Record<string, unknown> | undefined { return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : undefined; }
