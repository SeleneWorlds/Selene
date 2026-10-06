import { BrowserInputManager } from '@/core/BrowserInputManager';
import { BundleUiInputClaims } from '@/ui/BundleUiInputClaims';
import { ClientEntities } from '@/core/ClientEntities';
import type { EntitiesApi, ClientEntityScriptRunner, EntityApi } from '@/api/EntitiesApi';
import type { DebugState, RendererDebugOptions } from './DebugState';
import type { CameraApi } from '@/api/CameraApi';
import type { GameApi } from '@/api/GameApi';
import { ClientGrid } from '@/core/ClientGrid';
import type { GridApi } from '@/api/GridApi';
import type { InputApi } from '@/api/InputApi';
import { ClientMovementGrid } from '@/core/ClientMovementGrid';
import type { MovementGridApi } from '@/api/MovementGridApi';
import type { NetworkApi } from '@/api/NetworkApi';
import type { ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import { ClientCamera } from '@/core/ClientCamera';
import type { GameRenderer, GameRendererFactory } from '@/core/GameRenderer';
import type { NetworkClient } from '@/networking/NetworkClient';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import type { Coordinate } from '@/networking/GameProtocol';
import type { ClientMapTile } from '@/core/ClientMap';
import { ClientPacketDispatcher } from '@/core/ClientPacketDispatcher';
import { ClientTickPipeline } from '@/core/ClientTickPipeline';
import type { EnvironmentApi } from '@/api/EnvironmentApi';

export interface GameClientOptions {
  canvasHost: HTMLElement;
  debugState: DebugState;
  networkClient: NetworkClient;
  registries: ClientRegistrySnapshots;
  createRenderer: GameRendererFactory;
}

export class GameClient {
  private readonly camera = new ClientCamera();
  private readonly debugState: DebugState;
  private readonly entities: ClientEntities;
  private readonly grid: ClientGrid;
  private readonly inputManager: BrowserInputManager;
  private readonly bundleUiInput = new BundleUiInputClaims();
  private readonly movementGrid: ClientMovementGrid;
  private readonly networkClient: NetworkClient;
  private readonly packetDispatcher: ClientPacketDispatcher;
  private readonly renderer: GameRenderer;
  private readonly tickPipeline: ClientTickPipeline;
  private clientEntityScriptRemover: ((entityId: number) => void) | null = null;

  constructor(options: GameClientOptions) {
    this.debugState = options.debugState;
    this.grid = new ClientGrid(options.registries);
    this.inputManager = new BrowserInputManager(options.canvasHost, this.bundleUiInput);
    this.networkClient = options.networkClient;
    this.renderer = options.createRenderer({
      camera: this.camera,
      nameIdMappings: options.networkClient.nameIdMappings,
      grid: this.grid,
    });
    this.entities = new ClientEntities(
      options.registries,
      options.networkClient.nameIdMappings,
    );
    this.movementGrid = new ClientMovementGrid(this.entities, this.networkClient);
    this.packetDispatcher = new ClientPacketDispatcher(
      this.networkClient,
      this.grid,
      this.movementGrid,
      this.entities,
      this.renderer,
    );
    this.tickPipeline = new ClientTickPipeline(
      this.debugState,
      this.inputManager,
      this.movementGrid,
      this.entities,
      this.renderer,
    );
    this.entities.addListener({
      entityChanged: (snapshot) => this.renderer.upsertClientEntity(snapshot),
      entityRemoved: (id) => {
        this.clientEntityScriptRemover?.(id);
        this.renderer.removeClientEntity(id);
      },
      entityVisualAlphaChanged: (id, alpha) => this.renderer.setClientEntityAlpha(id, alpha),
    });
  }

  async start(): Promise<void> {
    this.debugState.update({ networkStatus: this.networkClient.status });
    await this.timeLoad('Renderer initialization', () => this.renderer.initialize());
    this.networkClient.addStatusListener(this.handleNetworkStatus);
    this.networkClient.addPacketListener(this.packetDispatcher.handlePacket);
    await this.timeLoad('Network connection', () => this.networkClient.connect());
    this.tickPipeline.start();
  }

  getCameraApi(): CameraApi {
    return {
      getCoordinate: () => this.camera.getCoordinate(),
      getPosition: () => this.camera.getPosition(),
      setPosition: (position) => {
        const coordinate = this.camera.setPosition(position);
        this.renderer.drawCameraScene();
        return coordinate;
      },
      setViewport: (x, y, width, height) => {
        this.camera.setViewportRect(x, y, width, height);
        this.renderer.drawCameraScene();
      },
      screenToWorld: (screenX, screenY) => {
        const logical = this.renderer.screenToLogical(screenX, screenY);
        return this.camera.screenToWorld(logical.x, logical.y);
      },
      addCoordinateChangedListener: (listener) => {
        return this.camera.addCoordinateChangedListener(listener);
      },
    };
  }

  getGameApi(): GameApi {
    return {
      addPreTickListener: (listener) => {
        return this.tickPipeline.addPreTickListener(listener);
      },
      setWindowAspectRatio: (width, height) => this.renderer.setWindowAspectRatio(width, height),
      clearWindowAspectRatio: () => this.renderer.clearWindowAspectRatio(),
      setOffscreenRendering: (width, height) => this.renderer.setOffscreenRendering(width, height),
      setNativeRendering: () => this.renderer.setNativeRendering(),
    };
  }

  getEnvironmentApi(): EnvironmentApi {
    return {
      setAmbientLight: color => this.renderer.setAmbientLight(color),
      getAmbientLight: () => this.renderer.getAmbientLight(),
      setTileLight: (coordinate, light) => this.renderer.setTileLight(coordinate, light),
      clearTileLights: () => this.renderer.clearTileLights(),
    };
  }

  setTimelineSoundPlayer(player: (sound: string, volume: number, pitch: number) => void): void {
    this.renderer.setTimelineSoundPlayer(player);
  }

  getGridApi(): GridApi {
    return this.grid;
  }

  getEntitiesApi(): EntitiesApi {
    return this.entities;
  }

  setClientEntityScriptRunner(
    runner: ClientEntityScriptRunner,
    remover?: (entityId: number) => void,
  ): void {
    this.tickPipeline.setClientEntityScriptRunner(runner);
    this.clientEntityScriptRemover = remover ?? null;
  }

  getInputApi(): InputApi {
    return this.inputManager;
  }

  getBundleUiInputClaims(): BundleUiInputClaims {
    return this.bundleUiInput;
  }

  getMapTiles(coordinate?: Coordinate, width?: number, height?: number): ClientMapTile[] {
    return this.renderer.getMapTiles(coordinate, width, height);
  }

  projectCoordinate(coordinate: Coordinate): { x: number; y: number } {
    return this.renderer.projectCoordinate(coordinate);
  }

  projectEntity(networkId: number): { x: number; y: number } | null {
    return this.renderer.projectEntity(networkId);
  }

  addMapChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void {
    return this.renderer.addMapChangedListener(listener);
  }

  getMovementGridApi(): MovementGridApi {
    return this.movementGrid;
  }

  getControlledEntity(): EntityApi | null {
    return this.movementGrid.getControlledEntity();
  }

  setRendererDebugOption(option: keyof RendererDebugOptions, enabled: boolean): void {
    if (option === 'cameraEvents') {
      this.camera.setCoordinateEventsEnabled(enabled);
    }
    this.renderer.setDebugOption(option, enabled);
  }

  setFitToScreen(enabled: boolean): void {
    this.renderer.setFitToScreen(enabled);
  }

  getNetworkApi(): NetworkApi {
    return this.packetDispatcher.getNetworkApi();
  }

  getNameIdMappings(): NameIdMappings {
    return this.networkClient.nameIdMappings;
  }

  private readonly handleNetworkStatus = (): void => {
    this.debugState.update({ networkStatus: this.networkClient.status });
  };

  private async timeLoad<T>(label: string, operation: () => Promise<T>): Promise<T> {
    const startedAt = performance.now();
    try {
      return await operation();
    } finally {
      this.debugState.recordLoadTiming(label, performance.now() - startedAt);
    }
  }

}
