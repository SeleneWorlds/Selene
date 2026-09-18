import type { EventsApi } from '@/api/EventsApi';
import type { HttpApi } from '@/api/HttpApi';
import type { I18nApi } from '@/api/I18nApi';
import type { RegistriesApi } from '@/api/RegistriesApi';
import type { SchedulesApi } from '@/api/SchedulesApi';
import type { TaskApi } from '@/api/TaskApi';
import type { LuaRuntime } from './LuaRuntime';
import { LuaArguments } from './LuaArguments';

export interface CommonLuaApis {
  events: EventsApi; tasks: TaskApi; schedules: SchedulesApi;
  http: HttpApi; registries: RegistriesApi; i18n: I18nApi;
}

export async function registerCommonLuaModules(runtime: LuaRuntime, apis: CommonLuaApis): Promise<void> {
  await runtime.preloadModule('selene.event', () => ({ of: (id: unknown) => apis.events.of(str('event.of', id, 'identifier')) }));
  await runtime.preloadModule('selene.task', () => ({ launch: (callback: unknown, ...args: unknown[]) => apis.tasks.launch(fn('task.launch', callback), args) }));
  await runtime.preloadModule('selene.schedules', () => ({
    everySecond: apis.schedules.everySecond, everyMinute: apis.schedules.everyMinute, everyHour: apis.schedules.everyHour,
    setTimeout: (ms: unknown, callback: unknown) => apis.schedules.setTimeout(int('schedules.setTimeout', ms), fn('schedules.setTimeout', callback)),
    clearTimeout: (id: unknown) => apis.schedules.clearTimeout(int('schedules.clearTimeout', id)),
    setInterval: (ms: unknown, callback: unknown, options?: unknown) => apis.schedules.setInterval(int('schedules.setInterval', ms), fn('schedules.setInterval', callback), record(options)?.immediate === true),
    clearInterval: (id: unknown) => apis.schedules.clearInterval(int('schedules.clearInterval', id)),
  }));
  await runtime.preloadModule('selene.http', () => ({ post: (url: unknown, body?: unknown, headers?: unknown) => apis.http.post(str('http.post', url, 'url'), body, record(headers)) }));
  await runtime.preloadModule('selene.registries', () => ({
    add: (registry: unknown, id: unknown, value: unknown) => apis.registries.add(str('registries.add', registry, 'registry'), str('registries.add', id, 'identifier'), value),
    findAll: (registry: unknown) => apis.registries.findAll(str('registries.findAll', registry, 'registry')),
    findByName: (registry: unknown, id: unknown) => apis.registries.findByName(str('registries.findByName', registry, 'registry'), str('registries.findByName', id, 'identifier')),
    findByMetadata: (registry: unknown, key: unknown, value: unknown) => apis.registries.findByMetadata(str('registries.findByMetadata', registry, 'registry'), str('registries.findByMetadata', key, 'key'), value),
  }));
  await runtime.preloadModule('selene.i18n', () => ({
    get: (key: unknown, locale?: unknown) => apis.i18n.get(str('i18n.get', key, 'key'), optStr(locale)),
    hasKey: (key: unknown, locale?: unknown) => apis.i18n.hasKey(str('i18n.hasKey', key, 'key'), optStr(locale)),
    format: (key: unknown, values?: unknown, locale?: unknown) => apis.i18n.format(str('i18n.format', key, 'key'), record(values), optStr(locale)),
  }));
}

function str(context: string, value: unknown, name: string) { return new LuaArguments(`selene.${context}`).string(value, name); }
function int(context: string, value: unknown) { return new LuaArguments(`selene.${context}`).integer(value, 'value'); }
function fn(context: string, value: unknown) { return new LuaArguments(`selene.${context}`).function(value, 'callback'); }
function optStr(value: unknown) { return value == null ? undefined : String(value); }
function record(value: unknown): Record<string, unknown> | undefined { return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : undefined; }
