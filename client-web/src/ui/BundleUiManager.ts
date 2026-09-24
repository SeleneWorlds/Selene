import type { NetworkApi, ClientNetworkPayload } from '@/api/NetworkApi';
import {
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
import {
  ClientUiIndexResponseSchema,
  parseServerResponse,
  type ClientUiEntrypoint,
} from '@/data/ClientServerResponseSchemas';

const API_VERSION = 5;
const MAX_PAYLOAD_ID_LENGTH = 128;
const MAX_PAYLOAD_BYTES = 64 * 1024;
const MAX_SUBSCRIPTIONS = 32767;
const MAX_VISUAL_IDENTIFIER_LENGTH = 256;

interface BundleUiManagerOptions {
  host: HTMLElement;
  serverApiUrl: string;
  authToken: string;
  network: NetworkApi;
  input: BundleUiInputClaims;
  camera: CameraApi;
  grid: GridApi;
  entities: EntitiesApi;
  getMapTiles: () => ClientMapTile[];
  onMapChanged: (listener: () => void) => () => void;
  registries: ClientRegistrySnapshots;
}
interface BundleUiModule {
  mount: (root: ShadowRoot, selene: BundleUiApi) => void | Promise<void>;
}
interface BundleUiApi {
  readonly apiVersion: number;
  readonly resolveAsset: (path: string) => string;
  readonly visuals: {
    getDefinition: (identifier: string) => Promise<ClientVisualDefinition>;
  };
  readonly storage: {
    load: (key: string) => Promise<string | null>;
    save: (key: string, value: string) => Promise<void>;
  };
  readonly input: {
    captureKeys: (...keys: string[]) => () => void;
    captureText: () => () => void;
    passThroughKeys: (...keys: string[]) => () => void;
    isPassthroughKey: (key: string) => boolean;
    onPointerDown: (callback: (event: BundleUiPointerEvent) => void) => () => void;
    onPointerUp: (callback: (event: BundleUiPointerEvent) => void) => () => void;
  };
  readonly network: {
    sendToServer: (payloadId: string, payload?: ClientNetworkPayload) => void;
    onPayload: (payloadId: string, callback: (payload: ClientNetworkPayload) => void) => () => void;
    onConnected: (callback: () => void) => () => void;
  };
  readonly world: {
    getCameraCoordinate: CameraApi['getCoordinate'];
    getMapTiles: () => ClientMapTile[];
    getEntitiesAt: (coordinate: Coordinate) => Promise<BundleUiWorldEntity[]>;
    onCameraCoordinateChanged: CameraApi['addCoordinateChangedListener'];
    onMapChanged: (callback: () => void) => () => void;
  };
}
interface BundleUiPointerEvent {
  clientX: number;
  clientY: number;
  button: number;
  shiftKey: boolean;
  coordinate: Coordinate;
}
interface BundleUiWorldEntity { networkId: number; tags: string[]; visual?: string }
export class BundleUiManager {
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
    const root = host.attachShadow({ mode: 'closed' });
    this.options.input.registerRoot(root);
    this.options.host.append(host);

    for (const style of parsed.querySelectorAll('style')) root.append(style.cloneNode(true));
    for (const link of parsed.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"][href]')) {
      const shadowLink = document.createElement('link');
      shadowLink.rel = 'stylesheet';
      shadowLink.href = new URL(link.getAttribute('href')!, entrypointUrl).href;
      root.append(shadowLink);
    }

    const content = document.createDocumentFragment();
    for (const child of [...parsed.body.childNodes]) content.append(child.cloneNode(true));
    root.append(content);

    const api = this.createApi(entrypointUrl, entrypoint);
    for (const script of parsed.querySelectorAll<HTMLScriptElement>('script[type="module"][src]')) {
      const moduleUrl = new URL(script.getAttribute('src')!, entrypointUrl).href;
      const module = await import(/* @vite-ignore */ moduleUrl) as Partial<BundleUiModule>;
      if (typeof module.mount !== 'function') {
        throw new Error(`${entrypoint.bundle}:${entrypoint.id} module must export mount(root, selene).`);
      }
      await module.mount(root, api);
    }
  }

  private createApi(entrypointUrl: string, entrypoint: ClientUiEntrypoint): BundleUiApi {
    let subscriptionCount = 0;
    const storagePrefix = `selene.bundle.${entrypoint.bundle}.${entrypoint.id}.`;
    return Object.freeze({
      apiVersion: API_VERSION,
      resolveAsset: (path: string) => {
        if (typeof path !== 'string' || path.length === 0) throw new Error('Asset path must be a non-empty string.');
        return new URL(path, entrypointUrl).href;
      },
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
      input: Object.freeze({
        captureKeys: (...keys: string[]) => this.options.input.captureKeys(...keys),
        captureText: () => this.options.input.captureText(),
        passThroughKeys: (...keys: string[]) => this.options.input.passThroughKeys(...keys),
        isPassthroughKey: (key: string) => this.options.input.isPassthroughKey(key),
        onPointerDown: (callback: (event: BundleUiPointerEvent) => void) =>
          this.registerPointerListener('pointerdown', callback),
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
        getMapTiles: () => this.options.getMapTiles(),
        getEntitiesAt: async (coordinate: Coordinate) => this.options.entities.getEntitiesAt(coordinate).map((entity) => {
          const definition = entity.getDefinition();
          const component = definition.components?.['illarion:visual'];
          const visual = component && typeof component === 'object'
            ? (component as Record<string, unknown>).visual
            : undefined;
          return {
            networkId: entity.getNetworkId(),
            tags: [...(definition.tags ?? [])],
            ...(typeof visual === 'string' ? { visual } : {}),
          };
        }),
        onCameraCoordinateChanged: (callback: Parameters<CameraApi['addCoordinateChangedListener']>[0]) => {
          if (typeof callback !== 'function') throw new Error('Camera callback must be a function.');
          return this.options.camera.addCoordinateChangedListener(callback);
        },
        onMapChanged: (callback: () => void) => {
          if (typeof callback !== 'function') throw new Error('Map callback must be a function.');
          return this.options.onMapChanged(callback);
        },
      }),
    });
  }

  private registerPointerListener(
    type: 'pointerdown' | 'pointerup',
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
