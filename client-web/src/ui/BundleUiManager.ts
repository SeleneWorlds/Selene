import type { ClientPreferencesApi } from '@/core/ClientPreferences';
import type { NetworkApi, ClientNetworkPayload } from '@/api/NetworkApi';
import {
  getRegistry,
  getRegistryEntry,
  resolveServerUrl,
  type ClientRegistrySnapshots,
} from '@/data/ClientRegistryLoader';
import type { BundleUiInputClaims } from './BundleUiInputClaims';
import type { CameraApi } from '@/api/CameraApi';
import type { GridApi } from '@/api/GridApi';
import type { EntitiesApi } from '@/api/EntitiesApi';
import type { Coordinate } from '@/networking/GameProtocol';
import type { ClientMapTile } from '@/core/ClientMap';
import type { ClientVisualDefinition } from '@/data/ClientRegistrySchemas';
import type { ClientAssetManifest } from '@/core/services/ClientAssetManifest';
import type { I18nApi } from '@/api/I18nApi';
import {
  ClientUiIndexResponseSchema,
  parseServerResponse,
  type ClientUiEntrypoint,
} from '@/data/ClientServerResponseSchemas';
import { getModAsset } from '@/core/services/ModAssetStore';

const API_VERSION = 18;
const MAX_PAYLOAD_ID_LENGTH = 128;
const MAX_PAYLOAD_BYTES = 64 * 1024;
const MAX_SUBSCRIPTIONS = 32767;
const MAX_VISUAL_IDENTIFIER_LENGTH = 256;

interface BundleUiManagerOptions {
  host: HTMLElement;
  serverApiUrl: string;
  authToken: string;
  assetManifest: ClientAssetManifest;
  network: NetworkApi;
  input: BundleUiInputClaims;
  camera: CameraApi;
  grid: GridApi;
  entities: EntitiesApi;
  getMapTiles: (coordinate?: Coordinate, width?: number, height?: number) => ClientMapTile[];
  hasTileAt: (coordinate: Coordinate) => boolean;
  setTileGridVisible: (visible: boolean) => void;
  projectCoordinate: (coordinate: Coordinate) => { x: number; y: number };
  projectEntity: (networkId: number) => { x: number; y: number } | null;
  getControlledEntity: () => ReturnType<EntitiesApi['getEntityByNetworkId']>;
  onMapChanged: (listener: (coordinate: Coordinate, width: number, height: number) => void) => () => void;
  registries: ClientRegistrySnapshots;
  i18n: I18nApi;
  preferences: ClientPreferencesApi;
}
interface BundleUiModule {
  mount: (root: ShadowRoot, selene: BundleUiApi) => void | Promise<void>;
}
interface BundleUiApi {
  readonly apiVersion: number;
  readonly launch: { getParameters(): Readonly<Record<string, string>> };
  readonly http: { request(path: string, payload?: ClientNetworkPayload, method?: 'GET' | 'POST' | 'PUT'): Promise<unknown> };
  readonly registries: {
    search(registry: string, query: string, lookup?: boolean): Promise<{
      registry: string; query: string; lookup: boolean;
      options: Array<{ value: string; label: string; data: unknown }>;
    } | null>;
  };
  readonly resolveAsset: (path: string) => Promise<string>;
  readonly i18n: I18nApi;
  readonly preferences: ClientPreferencesApi;
  readonly visuals: {
    getDefinition: (identifier: string) => Promise<ClientVisualDefinition>;
  };
  readonly storage: {
    load: (key: string) => Promise<string | null>;
    save: (key: string, value: string) => Promise<void>;
  };
  readonly ui: {
    setBundleVisible: (bundle: string, visible: boolean) => void;
  };
  readonly input: {
    captureKeys: (...keys: string[]) => () => void;
    captureText: () => () => void;
    passThroughKeys: (...keys: string[]) => () => void;
    isPassthroughKey: (key: string) => boolean;
    hasEditableFocus: () => boolean;
    onScroll: (callback: (event: { amountY: number }) => void) => () => void;
    onPointerDown: (callback: (event: BundleUiPointerEvent) => void) => () => void;
    onPointerMove: (callback: (event: BundleUiPointerEvent) => void) => () => void;
    onPointerUp: (callback: (event: BundleUiPointerEvent) => void) => () => void;
  };
  readonly network: {
    sendToServer: (payloadId: string, payload?: ClientNetworkPayload) => void;
    onPayload: (payloadId: string, callback: (payload: ClientNetworkPayload) => void) => () => void;
    onConnected: (callback: () => void) => () => void;
  };
  readonly world: {
    getCameraCoordinate: CameraApi['getCoordinate'];
    hasTileAt: (coordinate: Coordinate) => Promise<boolean>;
    setTileGridVisible: (visible: boolean) => Promise<void>;
    setCameraZoom: (zoom: number) => Promise<number>;
    getCameraPosition: () => Promise<ReturnType<CameraApi['getPosition']>>;
    setCameraPosition: (position: { x: number; y: number }) => Promise<Coordinate>;
    setViewport: (x: number, y: number, width: number, height: number) => void;
    getControlledEntity: () => BundleUiWorldEntity | null;
    getMapTiles: (coordinate?: Coordinate, width?: number, height?: number) => ClientMapTile[];
    projectCoordinate: (coordinate: Coordinate) => { x: number; y: number };
    getEntitiesAt: (coordinate: Coordinate) => Promise<BundleUiWorldEntity[]>;
    projectEntity: (networkId: number) => { x: number; y: number } | null;
    onCameraCoordinateChanged: CameraApi['addCoordinateChangedListener'];
    onMapChanged: (callback: (coordinate: Coordinate, width: number, height: number) => void) => () => void;
  };
}
interface BundleUiPointerEvent {
  clientX: number;
  clientY: number;
  button: number;
  shiftKey: boolean;
  coordinate: Coordinate;
}
interface BundleUiWorldEntity {
  networkId: number;
  coordinate: Coordinate;
  tags: string[];
  visual?: string;
  draggable: boolean;
  getComponent(name: string): unknown;
}
export class BundleUiManager {
  private readonly clientAssetUrls = new Map<string, Promise<string>>();
  private readonly bundleVisibility = new Map<string, boolean>();

  constructor(private readonly options: BundleUiManagerOptions) {}

  async load(): Promise<void> {
    const index = parseServerResponse(
      ClientUiIndexResponseSchema,
      await this.fetchJson('/client/ui', 'client UI index'),
      'client UI index',
    );
    await Promise.all(index.entrypoints.map((entrypoint) => this.mount(entrypoint)));
  }

  private async mount(entrypoint: ClientUiEntrypoint): Promise<void> {
    const entrypointUrl = resolveServerUrl(this.options.serverApiUrl, entrypoint.url);
    const response = await fetch(entrypointUrl);
    if (!response.ok) {
      throw new Error(`Failed to fetch ${entrypoint.bundle}:${entrypoint.id} UI: ${response.status} ${response.statusText}`);
    }

    const parsed = new DOMParser().parseFromString(await response.text(), 'text/html');
    const host = document.createElement('div');
    host.className = 'bundle-ui-host';
    host.dataset.bundle = entrypoint.bundle;
    host.dataset.entrypoint = entrypoint.id;
    host.hidden = this.bundleVisibility.get(entrypoint.bundle) === false;
    const root = host.attachShadow({ mode: 'closed' });
    this.options.input.registerRoot(root);
    this.options.host.append(host);

    for (const style of parsed.querySelectorAll('style')) root.append(style.cloneNode(true));
    for (const link of parsed.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"][href]')) {
      const stylesheetUrl = new URL(link.getAttribute('href')!, entrypointUrl).href;
      const response = await fetch(stylesheetUrl);
      if (!response.ok) {
        throw new Error(`Failed to fetch ${entrypoint.bundle}:${entrypoint.id} stylesheet: ${response.status} ${response.statusText}`);
      }
      const shadowStyle = document.createElement('style');
      shadowStyle.textContent = await response.text();
      root.append(shadowStyle);
    }

    const content = document.createDocumentFragment();
    for (const child of [...parsed.body.childNodes]) content.append(child.cloneNode(true));
    root.append(content);

    const api = this.createApi(entrypoint);
    for (const script of parsed.querySelectorAll<HTMLScriptElement>('script[type="module"][src]')) {
      const moduleUrl = new URL(script.getAttribute('src')!, entrypointUrl).href;
      const module = await import(/* @vite-ignore */ moduleUrl) as Partial<BundleUiModule>;
      if (typeof module.mount !== 'function') {
        throw new Error(`${entrypoint.bundle}:${entrypoint.id} module must export mount(root, selene).`);
      }
      await module.mount(root, api);
    }
  }

  private createApi(entrypoint: ClientUiEntrypoint): BundleUiApi {
    let subscriptionCount = 0;
    const storagePrefix = `selene.bundle.${entrypoint.bundle}.${entrypoint.id}.`;
    return Object.freeze({
      apiVersion: API_VERSION,
      launch: Object.freeze({
        getParameters: () => Object.freeze(Object.fromEntries(new URL(window.location.href).searchParams)),
      }),
      http: Object.freeze({
        request: async (path: string, payload: ClientNetworkPayload = {}, method: 'GET' | 'POST' | 'PUT' = 'GET') => {
          const segment = '[^/?#]+';
          const bundleRegistry = `/resources/bundles/${segment}/registries/${segment}`;
          const allowed = method === 'GET' ? [
            '^/resources/bundles$', `^/resources/bundles/${segment}/registries$`,
            `^${bundleRegistry}$`, `^/resources/files/${segment}(?:/${segment})*$`,
            `^/registries/${segment}/entries$`, '^/scripts$', '^/resources/changes$', '^/worldmap/image$',
          ] : method === 'POST' ? [
            '^/resources/permissions$', `^${bundleRegistry}/files$`, `^/resources/changes/(?:persist|discard)(?:/${segment})*$`,
          ] : method === 'PUT' ? [`^/resources/files/${segment}(?:/${segment})*$`] : [];
          if (!allowed.some((pattern) => new RegExp(pattern).test(path)) ||
            path.split('/').slice(1).some((part) => {
              const decoded = decodeURIComponent(part);
              return decoded === '.' || decoded === '..' || /[\\/\u0000-\u001f]/.test(decoded);
            })) throw new Error('Invalid resource HTTP path or method');
          const url = new URL(resolveServerUrl(this.options.serverApiUrl, path));
          if (method === 'GET') {
            for (const [key, value] of Object.entries(payload)) {
              if (value !== undefined && value !== null) url.searchParams.set(key, String(value));
            }
          }
          const response = await fetch(url, {
            method,
            headers: { Authorization: `Bearer ${this.options.authToken}`, 'Content-Type': 'application/json' },
            ...(method === 'GET' ? {} : { body: JSON.stringify(payload) }),
          });
          if (!response.ok) throw new Error(`HTTP operation failed: ${response.status} ${await response.text()}`);
          return response.json();
        },
      }),
      registries: Object.freeze({
        search: async (registry: string, query: string, lookup = false) => {
          const snapshot = getRegistry(this.options.registries, registry);
          if (!snapshot) return null;
          const options = Object.entries(snapshot.entries).flatMap(([value, entry]) => {
            const fields = entry as Record<string, unknown>;
            const metadata = (fields.metadata as Record<string, unknown> | undefined) ?? {};
            const label = String(metadata.name ?? fields.name ?? value);
            const matches = lookup ? value.toLowerCase() === query.toLowerCase()
              : label.toLowerCase().includes(query.toLowerCase()) || value.toLowerCase().includes(query.toLowerCase());
            return matches ? [{ value, label, data: entry }] : [];
          }).sort((a, b) => a.label < b.label ? -1 : a.label > b.label ? 1 : 0).slice(0, 50);
          return { registry, query, lookup, options };
        },
      }),
      resolveAsset: (path: string) => this.resolveAsset(path),
      preferences: Object.freeze({
        getLocale: () => this.options.preferences.getLocale(),
        setLocale: (locale: string) => this.options.preferences.setLocale(locale),
        onLocaleChanged: (listener: (locale: string) => void) => this.options.preferences.onLocaleChanged(listener),
      }),
      i18n: Object.freeze({
        get: (key: string, locale?: string) => this.options.i18n.get(key, locale),
        format: (key: string, parameters?: Record<string, unknown>, locale?: string) =>
          this.options.i18n.format(key, parameters, locale),
        hasKey: (key: string, locale?: string) => this.options.i18n.hasKey(key, locale),
      }),
      visuals: Object.freeze({
        getDefinition: async (identifier: string) => {
          requireVisualIdentifier(identifier);
          const definition = getRegistryEntry(this.options.registries, 'visuals', identifier);
          if (!definition) throw new Error(`Visual not found: ${identifier}`);
          return structuredClone(definition);
        },
      }),
      storage: Object.freeze({
        load: async (key: string) => window.localStorage.getItem(`${storagePrefix}${requireStorageKey(key)}`),
        save: async (key: string, value: string) => {
          if (typeof value !== 'string') throw new Error('Storage value must be a string.');
          window.localStorage.setItem(`${storagePrefix}${requireStorageKey(key)}`, value);
        },
      }),
      ui: Object.freeze({
        setBundleVisible: (bundle: string, visible: boolean) => this.setBundleVisible(bundle, visible),
      }),
      input: Object.freeze({
        captureKeys: (...keys: string[]) => this.options.input.captureKeys(...keys),
        captureText: () => this.options.input.captureText(),
        passThroughKeys: (...keys: string[]) => this.options.input.passThroughKeys(...keys),
        isPassthroughKey: (key: string) => this.options.input.isPassthroughKey(key),
        hasEditableFocus: () => this.options.input.hasEditableFocus(),
        onScroll: (callback: (event: { amountY: number }) => void) => {
          if (typeof callback !== 'function') throw new Error('Scroll callback must be a function.');
          const listener = (event: WheelEvent) => {
            if (this.options.input.consumesPointer(event)) return;
            const pixels = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? window.innerHeight : 1);
            callback({ amountY: pixels / 100 });
          };
          window.addEventListener('wheel', listener, { passive: true });
          return () => window.removeEventListener('wheel', listener);
        },
        onPointerDown: (callback: (event: BundleUiPointerEvent) => void) =>
          this.registerPointerListener('pointerdown', callback),
        onPointerMove: (callback: (event: BundleUiPointerEvent) => void) =>
          this.registerPointerListener('pointermove', callback),
        onPointerUp: (callback: (event: BundleUiPointerEvent) => void) =>
          this.registerPointerListener('pointerup', callback),
      }),
      network: Object.freeze({
        onConnected: (callback: () => void) => {
          if (typeof callback !== 'function') throw new Error('Connected callback must be a function.');
          return this.options.network.onConnected(callback);
        },
        sendToServer: (payloadId: string, payload: ClientNetworkPayload = {}) => {
          requirePayloadId(payloadId);
          requirePayload(payload);
          this.options.network.sendToServer(payloadId, payload);
        },
        onPayload: (payloadId: string, callback: (payload: ClientNetworkPayload) => void) => {
          requirePayloadId(payloadId);
          if (typeof callback !== 'function') throw new Error('Payload callback must be a function.');
          if (subscriptionCount >= MAX_SUBSCRIPTIONS) throw new Error('UI subscription limit reached.');
          subscriptionCount += 1;
          const unsubscribeNetwork = this.options.network.handlePayload(payloadId, callback);
          let active = true;
          const unsubscribe = () => {
            if (!active) return;
            active = false;
            subscriptionCount -= 1;
            unsubscribeNetwork();
          };
          return unsubscribe;
        },
      }),
      world: Object.freeze({
        getCameraCoordinate: () => this.options.camera.getCoordinate(),
        hasTileAt: async (coordinate: Coordinate) => this.options.hasTileAt(requireCoordinate(coordinate)),
        setTileGridVisible: async (visible: boolean) => {
          if (typeof visible !== 'boolean') throw new Error('Grid visibility must be a boolean.');
          this.options.setTileGridVisible(visible);
        },
        setCameraZoom: async (zoom: number) => this.options.camera.setZoom(zoom),
        getCameraPosition: async () => this.options.camera.getPosition(),
        setCameraPosition: async (position: { x: number; y: number }) => {
          if (!position || !Number.isFinite(position.x) || !Number.isFinite(position.y)) {
            throw new Error('Camera position must contain finite x and y values.');
          }
          return this.options.camera.setPosition({ x: position.x, y: position.y });
        },
        setViewport: (x: number, y: number, width: number, height: number) => {
          if (![x, y, width, height].every(Number.isSafeInteger) || width <= 0 || height <= 0) {
            throw new Error('Viewport must contain integer x and y coordinates and positive integer dimensions.');
          }
          this.options.camera.setViewport(x, y, width, height);
        },
        getControlledEntity: () => {
          const entity = this.options.getControlledEntity();
          return entity ? this.toWorldEntity(entity) : null;
        },
        getMapTiles: (coordinate?: Coordinate, width?: number, height?: number) =>
          this.options.getMapTiles(coordinate, width, height),
        projectCoordinate: (coordinate: Coordinate) => this.options.projectCoordinate(requireCoordinate(coordinate)),
        getEntitiesAt: async (coordinate: Coordinate) =>
          this.options.entities.getEntitiesAt(coordinate).map((entity) => this.toWorldEntity(entity)),
        projectEntity: (networkId: number) => {
          if (!Number.isSafeInteger(networkId)) throw new Error('Entity network ID must be an integer.');
          return this.options.projectEntity(networkId);
        },
        onCameraCoordinateChanged: (callback: Parameters<CameraApi['addCoordinateChangedListener']>[0]) => {
          if (typeof callback !== 'function') throw new Error('Camera callback must be a function.');
          return this.options.camera.addCoordinateChangedListener(callback);
        },
        onMapChanged: (callback: (coordinate: Coordinate, width: number, height: number) => void) => {
          if (typeof callback !== 'function') throw new Error('Map callback must be a function.');
          return this.options.onMapChanged(callback);
        },
      }),
    });
  }

  private setBundleVisible(bundle: string, visible: boolean): void {
    if (typeof bundle !== 'string' || bundle.length === 0) {
      throw new Error('Bundle name must be a non-empty string.');
    }
    if (typeof visible !== 'boolean') {
      throw new Error('Bundle visibility must be a boolean.');
    }
    this.bundleVisibility.set(bundle, visible);
    for (const host of this.options.host.querySelectorAll<HTMLElement>('.bundle-ui-host')) {
      if (host.dataset.bundle === bundle) host.hidden = !visible;
    }
  }

  private toWorldEntity(entity: NonNullable<ReturnType<EntitiesApi['getEntityByNetworkId']>>): BundleUiWorldEntity {
    const definition = entity.getDefinition();
    const components = Object.values(definition.components ?? {});
    const component = components.find((value) => value !== null
      && typeof value === 'object'
      && (value as Record<string, unknown>).type === 'visual');
    const visual = (component as Record<string, unknown> | undefined)?.visual;
    const draggable = Object.values(entity.getComponents()).some((value) => {
      return value !== null
        && typeof value === 'object'
        && (value as Record<string, unknown>).type === 'draggable'
        && (value as Record<string, unknown>).enabled !== false;
    });
    return {
      networkId: entity.getNetworkId(),
      coordinate: entity.getCoordinate(),
      tags: [...(definition.tags ?? [])],
      draggable,
      getComponent: (name) => entity.getComponent(name),
      ...(typeof visual === 'string' ? { visual } : {}),
    };
  }

  private registerPointerListener(
    type: 'pointerdown' | 'pointermove' | 'pointerup',
    callback: (event: BundleUiPointerEvent) => void,
  ): () => void {
    if (typeof callback !== 'function') throw new Error('Pointer callback must be a function.');
    const listener = (event: PointerEvent) => {
      if (this.options.input.consumesPointer(event)) return;
      const world = this.options.camera.screenToWorld(event.clientX, event.clientY);
      const z = this.options.camera.getCoordinate().z;
      callback({
        clientX: event.clientX,
        clientY: event.clientY,
        button: event.button,
        shiftKey: event.shiftKey,
        coordinate: this.options.grid.screenToCoordinate(world.x, world.y, z),
      });
    };
    window.addEventListener(type, listener, true);
    return () => window.removeEventListener(type, listener, true);
  }

  private resolveAsset(path: string): Promise<string> {
    const normalized = path.replace(/^\/+/, '');
    if (!normalized || normalized.includes('..')) {
      return Promise.reject(new Error('Client asset path must be a non-empty relative path.'));
    }

    let url = this.clientAssetUrls.get(normalized);
    if (!url) {
      url = this.fetchAsset(normalized);
      this.clientAssetUrls.set(normalized, url);
    }
    return url;
  }

  private async fetchAsset(path: string): Promise<string> {
    const modAsset = await getModAsset(path);
    if (modAsset) return URL.createObjectURL(modAsset);
    const assetPath = this.options.assetManifest.assets[path];
    if (!assetPath) throw new Error(`Client asset is missing: ${path}`);

    const response = await fetch(resolveServerUrl(this.options.serverApiUrl, assetPath), {
      headers: { Authorization: `Bearer ${this.options.authToken}` },
    });
    if (!response.ok) throw new Error(`Failed to fetch client asset: ${response.status} ${response.statusText}`);
    return URL.createObjectURL(await response.blob());
  }

  private async fetchJson(path: string, label: string): Promise<unknown> {
    const response = await fetch(resolveServerUrl(this.options.serverApiUrl, path), {
      headers: { Authorization: `Bearer ${this.options.authToken}` },
    });
    if (!response.ok) throw new Error(`Failed to fetch ${label}: ${response.status} ${response.statusText}`);
    return response.json();
  }
}

function requireStorageKey(value: string): string {
  if (typeof value !== 'string' || value.length === 0 || value.length > 128 || !/^[a-zA-Z0-9._-]+$/.test(value)) {
    throw new Error('Storage key must contain 1-128 letters, numbers, dots, underscores, or hyphens.');
  }
  return value;
}

function requireCoordinate(value: Coordinate): Coordinate {
  if (
    !value ||
    typeof value !== 'object' ||
    !Number.isFinite(value.x) ||
    !Number.isFinite(value.y) ||
    !Number.isFinite(value.z)
  ) {
    throw new Error('Coordinate must contain finite x, y, and z values.');
  }
  return { x: value.x, y: value.y, z: value.z };
}

function requirePayloadId(value: string): void {
  if (typeof value !== 'string' || value.length === 0 || value.length > MAX_PAYLOAD_ID_LENGTH) {
    throw new Error(`Payload ID must contain 1-${MAX_PAYLOAD_ID_LENGTH} characters.`);
  }
}
function requireVisualIdentifier(value: string): void {
  if (typeof value !== 'string' || value.length === 0 || value.length > MAX_VISUAL_IDENTIFIER_LENGTH) {
    throw new Error(`Visual identifier must contain 1-${MAX_VISUAL_IDENTIFIER_LENGTH} characters.`);
  }
}
function requirePayload(payload: ClientNetworkPayload): void {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) throw new Error('Payload must be a JSON object.');
  if (new TextEncoder().encode(JSON.stringify(payload)).byteLength > MAX_PAYLOAD_BYTES) {
    throw new Error(`Payload exceeds ${MAX_PAYLOAD_BYTES} bytes.`);
  }
}
