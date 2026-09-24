import { readJoinToken } from '@/auth/JoinToken';
import { createDebugState, type DebugState, type RendererDebugOptions } from '@/core/DebugState';
import { GameClient } from '@/core/GameClient';
import { loadClientAssetManifest } from '@/core/services/ClientAssetManifest';
import { EventsService } from '@/core/services/EventsService';
import { HttpService } from '@/core/services/HttpService';
import { I18nService } from '@/core/services/I18nService';
import { MapService } from '@/core/services/MapService';
import { RegistriesService } from '@/core/services/RegistriesService';
import { ResourcesService } from '@/core/services/ResourcesService';
import { SchedulesService } from '@/core/services/SchedulesService';
import { SoundsService } from '@/core/services/SoundsService';
import { TaskService } from '@/core/services/TaskService';
import { TexturesService } from '@/core/services/TexturesService';
import { VisualsService } from '@/core/services/VisualsService';
import { loadClientRegistries } from '@/data/ClientRegistryLoader';
import { WebSocketNetworkClient } from '@/networking/WebSocketNetworkClient';
import { PixiGameRenderer } from '@/renderer/PixiGameRenderer';
import { registerCameraLuaModule } from '@/scripting/CameraLuaBindings';
import { registerClientFeatureLuaModules } from '@/scripting/ClientFeatureLuaBindings';
import { registerCommonLuaModules } from '@/scripting/CommonLuaBindings';
import { registerEntitiesLuaModule, toLuaEntity } from '@/scripting/EntitiesLuaBindings';
import { registerGameLuaModule } from '@/scripting/GameLuaBindings';
import { registerGridLuaModule } from '@/scripting/GridLuaBindings';
import { registerInputLuaModule } from '@/scripting/InputLuaBindings';
import { loadAndRunClientLua } from '@/scripting/ClientLuaLoader';
import { LuaRuntime } from '@/scripting/LuaRuntime';
import { registerMovementGridLuaModule } from '@/scripting/MovementGridLuaBindings';
import { registerNetworkLuaModule } from '@/scripting/NetworkLuaBindings';
import { BundleUiManager } from '@/ui/BundleUiManager';

export interface BootstrapClientOptions {
  viewportHost: HTMLElement;
  bundleUiHost: HTMLElement;
  fitToScreen: boolean;
  debugState?: DebugState;
}

export interface WebClientRuntime {
  setFitToScreen(enabled: boolean): void;
  setRendererDebugOption(option: keyof RendererDebugOptions, enabled: boolean): void;
}

class DefaultWebClientRuntime implements WebClientRuntime {
  private gameClient: GameClient | null = null;

  constructor(private readonly debugState: DebugState) {}

  setFitToScreen(enabled: boolean): void {
    this.gameClient?.setFitToScreen(enabled);
  }

  setRendererDebugOption(option: keyof RendererDebugOptions, enabled: boolean): void {
    this.gameClient?.setRendererDebugOption(option, enabled);
  }

  async start(options: BootstrapClientOptions): Promise<void> {
    const loadingStartedAt = performance.now();
    const serverApiUrl = configuredUrl(import.meta.env.VITE_SELENE_SERVER_API_URL) ?? window.location.origin;
    const webSocketUrl = configuredUrl(import.meta.env.VITE_SELENE_WEBSOCKET_URL)
      ?? await loadWebSocketUrl(serverApiUrl);
    const authToken = readJoinToken();

    const registries = await this.timeLoad('Registry loading', () =>
      loadClientRegistries({ serverApiUrl, authToken }),
    );
    const assetManifest = await loadClientAssetManifest({ serverApiUrl, authToken });

    const gameClient = new GameClient({
      canvasHost: options.viewportHost,
      debugState: this.debugState,
      registries,
      createRenderer: ({ camera, grid, nameIdMappings }) => new PixiGameRenderer({
        host: options.viewportHost,
        uiHost: options.bundleUiHost,
        camera,
        grid,
        nameIdMappings,
        debugState: this.debugState,
        registries,
        assetManifest,
        serverApiUrl,
        authToken,
      }),
      networkClient: new WebSocketNetworkClient({
        url: webSocketUrl,
        authToken: () => Promise.resolve(authToken),
      }),
    });
    this.gameClient = gameClient;
    gameClient.setFitToScreen(options.fitToScreen);

    const mapApiOptions = {
      nameIdMappings: gameClient.getNameIdMappings(),
      getMapTiles: () => gameClient.getMapTiles(),
      onMapChanged: (listener: Parameters<GameClient['addMapChangedListener']>[0]) =>
        gameClient.addMapChangedListener(listener),
    };
    const registriesApi = new RegistriesService(registries, gameClient.getNameIdMappings());
    const visualsApi = new VisualsService(registriesApi);
    const schedulesService = new SchedulesService();
    const soundsService = new SoundsService(registriesApi, assetManifest, serverApiUrl);
    const entityApis = { registries: registriesApi, visuals: visualsApi };
    const commonApis = {
      events: new EventsService(),
      tasks: new TaskService(),
      schedules: schedulesService,
      http: new HttpService(),
      registries: registriesApi,
      i18n: new I18nService(registries),
    };
    const featureApis = {
      map: new MapService(mapApiOptions, registriesApi, visualsApi),
      sounds: soundsService,
      resources: new ResourcesService(assetManifest, serverApiUrl, authToken),
      textures: new TexturesService(),
      visuals: visualsApi,
    };

    const luaRuntime = new LuaRuntime();
    await this.timeLoad('Lua bindings', async () => {
      await registerCameraLuaModule(luaRuntime, gameClient.getCameraApi());
      await registerEntitiesLuaModule(luaRuntime, gameClient.getEntitiesApi(), entityApis);
      await registerGameLuaModule(luaRuntime, gameClient.getGameApi());
      await registerGridLuaModule(luaRuntime, gameClient.getGridApi());
      await registerInputLuaModule(luaRuntime, gameClient.getInputApi());
      await registerMovementGridLuaModule(luaRuntime, gameClient.getMovementGridApi());
      await registerNetworkLuaModule(luaRuntime, gameClient.getNetworkApi());
      await registerCommonLuaModules(luaRuntime, commonApis);
      await registerClientFeatureLuaModules(luaRuntime, featureApis);
    });
    await this.timeLoad('Client Lua loading', () =>
      loadAndRunClientLua({ serverApiUrl, authToken, runtime: luaRuntime }),
    );
    gameClient.setClientEntityScriptRunner(
      (scriptModule, entityId, entity, deltaSeconds) => {
        luaRuntime.runClientEntityScript(
          scriptModule,
          entityId,
          toLuaEntity(entity, entityApis),
          deltaSeconds,
        );
      },
      (entityId) => luaRuntime.removeClientEntityScript(entityId),
    );

    const bundleUiManager = new BundleUiManager({
      host: options.bundleUiHost,
      serverApiUrl,
      authToken,
      network: gameClient.getNetworkApi(),
      input: gameClient.getBundleUiInputClaims(),
      camera: gameClient.getCameraApi(),
      grid: gameClient.getGridApi(),
      entities: gameClient.getEntitiesApi(),
      registries,
      getMapTiles: () => gameClient.getMapTiles(),
      onMapChanged: (listener) => gameClient.addMapChangedListener(listener),
    });
    await this.timeLoad('Bundle UI loading', () => bundleUiManager.load());
    await gameClient.start();

    this.debugState.update({ loadTotalMs: performance.now() - loadingStartedAt });
    console.info(`[Timing] Total startup: ${this.debugState.snapshot.value.loadTotalMs.toFixed(1)} ms`);
  }

  private async timeLoad<T>(label: string, operation: () => Promise<T>): Promise<T> {
    const startedAt = performance.now();
    try {
      return await operation();
    } finally {
      this.debugState.recordLoadTiming(label, performance.now() - startedAt);
    }
  }
}

export async function bootstrapClient(options: BootstrapClientOptions): Promise<WebClientRuntime> {
  const runtime = new DefaultWebClientRuntime(options.debugState ?? createDebugState());
  await runtime.start(options);
  return runtime;
}

function configuredUrl(value: string | undefined): string | null {
  const trimmedValue = value?.trim();
  return trimmedValue || null;
}

interface WebClientConfig {
  webSocketUrl?: unknown;
  webSocketPort?: unknown;
}

async function loadWebSocketUrl(serverApiUrl: string): Promise<string> {
  const response = await fetch(new URL('/client/config', ensureTrailingSlash(serverApiUrl)));
  if (!response.ok) {
    throw new Error(`Could not load web client configuration (${response.status} ${response.statusText}).`);
  }

  const config = await response.json() as WebClientConfig;
  if (typeof config.webSocketUrl === 'string' && config.webSocketUrl.trim()) {
    return config.webSocketUrl.trim();
  }
  if (!Number.isInteger(config.webSocketPort) || (config.webSocketPort as number) < 1) {
    throw new Error('The server returned an invalid WebSocket configuration.');
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.hostname}:${String(config.webSocketPort)}/ws`;
}

function ensureTrailingSlash(url: string): string {
  return url.endsWith('/') ? url : `${url}/`;
}
