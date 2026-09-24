export interface ClientSettings {
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
}

const SETTINGS_STORAGE_KEY = 'selene.client.settings';
const defaultSettings: ClientSettings = {
  debugOverlayVisible: false,
  fitToScreen: true,
};

interface SettingsStore {
  load(): Promise<ClientSettings>;
  save(settings: ClientSettings): Promise<void>;
}

let currentSettings = { ...defaultSettings };
let initialization: Promise<void> | null = null;
let store: SettingsStore | null = null;

/** Must finish before Vue mounts so persisted settings are applied on first render. */
export function initializeSettings(): Promise<void> {
  initialization ??= (async () => {
    store = createSettingsStore();
    currentSettings = await store.load();
  })().catch((error: unknown) => {
    console.warn('Could not load client settings.', error);
  });

  return initialization;
}

export function loadSettings(): ClientSettings {
  return { ...currentSettings };
}

export function saveSettings(settings: ClientSettings): void {
  currentSettings = sanitizeSettings(settings);
  void (store ?? createSettingsStore()).save(currentSettings).catch((error: unknown) => {
    console.warn('Could not persist client settings.', error);
  });
}

function createSettingsStore(): SettingsStore {
  return new LocalStorageSettingsStore();
}

class LocalStorageSettingsStore implements SettingsStore {
  async load(): Promise<ClientSettings> {
    const storedSettings = window.localStorage.getItem(SETTINGS_STORAGE_KEY);
    return storedSettings ? sanitizeSettings(JSON.parse(storedSettings)) : { ...defaultSettings };
  }

  async save(settings: ClientSettings): Promise<void> {
    window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function sanitizeSettings(value: unknown): ClientSettings {
  return {
    debugOverlayVisible:
      isRecord(value) && typeof value.debugOverlayVisible === 'boolean'
        ? value.debugOverlayVisible
        : defaultSettings.debugOverlayVisible,
    fitToScreen:
      isRecord(value) && typeof value.fitToScreen === 'boolean'
        ? value.fitToScreen
        : defaultSettings.fitToScreen,
  };
}
