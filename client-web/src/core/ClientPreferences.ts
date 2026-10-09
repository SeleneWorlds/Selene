import { loadSettings, saveSettings } from '@/ui/settings';
import type { WebSocketNetworkClient } from '@/networking/WebSocketNetworkClient';

export interface ClientPreferencesApi {
  getLocale(): string;
  setLocale(locale: string): void;
  onLocaleChanged(listener: (locale: string) => void): () => void;
}

export class ClientPreferences implements ClientPreferencesApi {
  private locale = loadSettings().locale;
  private readonly listeners = new Set<(locale: string) => void>();

  constructor(private readonly network: WebSocketNetworkClient) {
    network.setLocale(this.locale);
  }

  getLocale(): string { return this.locale; }

  setLocale(locale: string): void {
    const normalized = Intl.getCanonicalLocales(locale.replace(/_/g, '-'))[0]?.replace(/-/g, '_');
    if (!normalized) throw new Error('Locale must not be empty.');
    if (normalized === this.locale) return;
    this.network.setLocale(normalized);
    this.locale = normalized;
    saveSettings({ ...loadSettings(), locale: normalized });
    for (const listener of this.listeners) listener(normalized);
  }

  onLocaleChanged(listener: (locale: string) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}
