import type { I18nApi } from '@/api/I18nApi';
import { getRegistry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import { isRecord } from './utils';

export class I18nService implements I18nApi {
  constructor(private readonly snapshots: ClientRegistrySnapshots) {}
  get(key: string, locale = navigator.language) { for (const entry of Object.values(getRegistry(this.snapshots, 'messages')?.entries ?? {})) { if (!isRecord(entry)) continue; const localized = entry[key]; if (typeof localized === 'string') return localized; if (isRecord(localized)) { const value = localized[locale] ?? localized[locale.split(/[-_]/)[0]] ?? localized.en; if (typeof value === 'string') return value; } } return null; }
  format(key: string, parameters: Record<string, unknown> = {}, locale?: string) { const template = this.get(key, locale); return template?.replace(/\{([^{}]+)\}/g, (match, name: string) => name in parameters ? String(parameters[name]) : match) ?? null; }
  hasKey(key: string, locale?: string) { return this.get(key, locale) !== null; }
}
