import { acquireSessionToken, restartAuthorization } from '@/auth/AuthorizationFlow';
import { createDebugState, type DebugState, type RendererDebugOptions } from '@/core/DebugState';
import { GameClient } from '@/core/GameClient';
import { loadClientAssetManifest } from '@/core/services/ClientAssetManifest';
import { applyModRegistryOverrides } from '@/core/services/ModAssetStore';
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
import type { Coordinate } from '@/networking/GameProtocol';
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
  onStartupProgress?: (progress: StartupProgress) => void;
  onDisconnected?: (reason: string) => void;
}

export interface StartupProgress {
  label: string;
  progress: number;
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
    const reportProgress = (label: string, progress: number): void => {
      options.onStartupProgress?.({ label, progress });
    };

    reportProgress('Connecting to server', 0.08);
    const serverApiUrl = window.location.origin;
    const webSocketUrl = import.meta.env.DEV
      ? developmentWebSocketUrl()
      : await loadWebSocketUrl(serverApiUrl);
    const joinToken = await acquireSessionToken(serverApiUrl);

    reportProgress('Waiting to join', 0.12);
    let authToken: string;
    try {
      authToken = await joinServer(serverApiUrl, joinToken);
    } catch (error) {
      if (error instanceof ClientJoinError && error.status === 401) await restartAuthorization(serverApiUrl);
      throw error;
    }
    startSessionRenewal(serverApiUrl, authToken);

    reportProgress('Loading game data', 0.2);
    const serverRegistries = await this.timeLoad('Registry loading', () =>
      loadClientRegistries({ serverApiUrl, authToken }),
    );
    const registries = await applyModRegistryOverrides(serverRegistries);
    reportProgress('Loading assets', 0.42);
    const assetManifest = await loadClientAssetManifest({ serverApiUrl, authToken });

    reportProgress('Preparing renderer', 0.55);
    const networkClient = new WebSocketNetworkClient({
      url: webSocketUrl,
      authToken: () => Promise.resolve(authToken),
      onDisconnected: options.onDisconnected,
    });
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
      networkClient,
    });
    this.gameClient = gameClient;
    gameClient.setFitToScreen(options.fitToScreen);

    const mapApiOptions = {
      nameIdMappings: gameClient.getNameIdMappings(),
      getMapTiles: (coordinate?: Coordinate, width?: number, height?: number) =>
        gameClient.getMapTiles(coordinate, width, height),
      onMapChanged: (listener: Parameters<GameClient['addMapChangedListener']>[0]) =>
        gameClient.addMapChangedListener(listener),
    };
    const registriesApi = new RegistriesService(registries, gameClient.getNameIdMappings());
    const visualsApi = new VisualsService(registriesApi);
    const schedulesService = new SchedulesService();
    const soundsService = new SoundsService(registriesApi, assetManifest, serverApiUrl, authToken);
    networkClient.addPacketListener(soundsService.handlePacket);
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
    reportProgress('Preparing game scripts', 0.68);
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
    reportProgress('Loading game scripts', 0.78);
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
      assetManifest,
      network: gameClient.getNetworkApi(),
      input: gameClient.getBundleUiInputClaims(),
      camera: gameClient.getCameraApi(),
      grid: gameClient.getGridApi(),
      entities: gameClient.getEntitiesApi(),
      registries,
      getMapTiles: (coordinate?: Coordinate, width?: number, height?: number) =>
        gameClient.getMapTiles(coordinate, width, height),
      projectCoordinate: (coordinate) => gameClient.projectCoordinate(coordinate),
      projectEntity: (networkId) => gameClient.projectEntity(networkId),
      onMapChanged: (listener) => gameClient.addMapChangedListener(listener),
    });
    reportProgress('Loading interface', 0.88);
    await this.timeLoad('Bundle UI loading', () => bundleUiManager.load());
    reportProgress('Joining game', 0.96);
    await gameClient.start();

    this.debugState.update({ loadTotalMs: performance.now() - loadingStartedAt });
    reportProgress('Connected', 1);
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

export class ClientJoinError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = 'ClientJoinError';
  }
}

interface JoinResponse {
  status?: unknown;
  message?: unknown;
  token?: unknown;
}

async function joinServer(serverApiUrl: string, authToken: string): Promise<string> {
  const deadline = Date.now() + 60_000;
  while (Date.now() <= deadline) {
    const response = await fetch(new URL('/join', ensureTrailingSlash(serverApiUrl)), {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` },
    });
    if (!response.ok) {
      throw new ClientJoinError(`The server rejected the join request (${response.status} ${response.statusText}).`, response.status);
    }

    const join = await response.json() as JoinResponse;
    if (join.status === 'Accepted') {
      if (typeof join.token !== 'string' || !join.token) {
        throw new ClientJoinError('The server accepted the join without returning a game session.');
      }
      return join.token;
    }
    if (join.status === 'Rejected') {
      throw new ClientJoinError(typeof join.message === 'string' ? join.message : 'The join request was rejected.');
    }
    if (join.status !== 'Pending') {
      throw new ClientJoinError('The server returned an invalid join status.');
    }
    await new Promise(resolve => window.setTimeout(resolve, 2_000));
  }
  throw new ClientJoinError('Timed out waiting for the server to accept the join request.');
}

function startSessionRenewal(serverApiUrl: string, authToken: string): void {
  const renew = async (): Promise<void> => {
    try {
      const response = await fetch(new URL('/session/renew', ensureTrailingSlash(serverApiUrl)), {
        method: 'POST',
        headers: { Authorization: `Bearer ${authToken}` },
      });
      if (!response.ok) console.warn(`Could not renew the game session (${response.status} ${response.statusText}).`);
    } catch (error) {
      console.warn('Could not renew the game session.', error);
    }
  };
  window.setInterval(() => void renew(), 30 * 60 * 1_000);
}

function developmentWebSocketUrl(): string {
  const url = new URL('/ws', window.location.origin);
  url.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
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
