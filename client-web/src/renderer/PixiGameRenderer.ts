import { Application, Container } from 'pixi.js';
import type { ClientEntitySnapshot } from '@/api/EntitiesApi';
import type { DebugState, RendererDebugOptions } from '@/core/DebugState';
import type { ClientAssetManifest } from '@/core/services/ClientAssetManifest';
import type { ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type { ClientCamera } from '@/core/ClientCamera';
import type { GameRenderer } from '@/core/GameRenderer';
import { InteriorFadeController } from '@/renderer/InteriorFadeController';
import { PixiScene } from '@/renderer/PixiScene';
import { RenderPipeline } from '@/renderer/RenderPipeline';
import { PixiViewport, RENDER_RESOLUTION } from '@/renderer/PixiViewport';
import { ContentTextureLoader } from '@/renderer/entities/ContentTextureLoader';
import { EntityVisualResolver } from '@/renderer/entities/EntityVisualResolver';
import { PixiEntityLayer } from '@/renderer/entities/PixiEntityLayer';
import { PixiTilemapLayer } from '@/renderer/tiles/PixiTilemapLayer';
import type { ClientMapTile } from '@/core/ClientMap';
import { TileVisualResolver } from '@/renderer/tiles/TileVisualResolver';
import type { Coordinate, GamePacket } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import type { ClientGrid } from '@/core/ClientGrid';
import { projectCoordinate as projectWorldCoordinate } from '@/core/WorldProjection';
import { LightingEnvironment } from './LightingEnvironment';
import type { LightColor, TileLight } from '@/api/EnvironmentApi';
import { TimelinePlayer } from '@/renderer/timeline/TimelinePlayer';

export interface PixiGameRendererOptions {
  host: HTMLElement;
  uiHost: HTMLElement;
  camera: ClientCamera;
  debugState: DebugState;
  registries: ClientRegistrySnapshots;
  assetManifest: ClientAssetManifest;
  serverApiUrl: string;
  authToken: string;
  nameIdMappings: NameIdMappings;
  grid: ClientGrid;
}

export class PixiGameRenderer implements GameRenderer {
  private readonly app = new Application();
  private readonly debugState: DebugState;
  private readonly host: HTMLElement;
  private readonly camera: ClientCamera;
  private readonly textureLoader: ContentTextureLoader;
  private readonly tilemapLayer: PixiTilemapLayer;
  private readonly entityLayer: PixiEntityLayer;
  private readonly scene: PixiScene;
  private readonly viewport: PixiViewport;
  private readonly pipeline: RenderPipeline;
  private readonly lighting = new LightingEnvironment();
  private readonly timelinePlayer: TimelinePlayer;
  private readonly screenEffects = new Container();

  private rendererOptions: RendererDebugOptions;

  constructor(options: PixiGameRendererOptions) {
    this.host = options.host;
    this.camera = options.camera;
    this.debugState = options.debugState;
    this.rendererOptions = { ...options.debugState.snapshot.value.rendererOptions };
    this.textureLoader = new ContentTextureLoader(
      options.assetManifest,
      options.serverApiUrl,
      options.authToken,
    );
    this.scene = new PixiScene(this.camera);
    this.tilemapLayer = new PixiTilemapLayer(
      new TileVisualResolver(options.registries, options.nameIdMappings),
      this.textureLoader,
      this.scene.depthSortedContainer,
      this.lighting,
    );
    this.timelinePlayer = new TimelinePlayer(
      options.registries,
      this.textureLoader,
      this.scene.depthSortedContainer,
      this.screenEffects,
      coordinate => this.tilemapLayer.getSurface(coordinate),
      () => this.camera.getViewportRect(),
    );
    this.entityLayer = new PixiEntityLayer(
      new EntityVisualResolver(options.registries, options.nameIdMappings),
      this.textureLoader,
      options.grid,
      this.scene.depthSortedContainer,
      coordinate => this.tilemapLayer.getSurface(coordinate),
      this.lighting,
    );
    this.viewport = new PixiViewport(
      this.app, options.host, options.uiHost, this.camera, this.debugState, () => this.drawCameraScene(),
    );
    this.pipeline = new RenderPipeline(
      this.app, this.debugState, this.scene, this.viewport, this.entityLayer, this.tilemapLayer,
      new InteriorFadeController(this.tilemapLayer, this.entityLayer),
      this.lighting,
    );
  }

  async initialize(): Promise<void> {
    this.debugState.update({ rendererStatus: 'initializing' });

    await this.app.init({
      antialias: false,
      autoDensity: true,
      backgroundAlpha: 0,
      resizeTo: this.host,
      resolution: RENDER_RESOLUTION,
      culler: {
        updateTransform: false,
      },
    });
    this.app.ticker.stop();
    this.host.appendChild(this.app.canvas);
    this.app.stage.addChild(this.scene.container);
    this.app.stage.addChild(this.screenEffects);
    this.viewport.initialize();
    this.debugState.update({ rendererStatus: 'ready' });
  }

  setTimelineSoundPlayer(player: (sound: string, volume: number, pitch: number) => void): void {
    this.timelinePlayer.setSoundPlayer(player);
  }

  playTimelineAt(
    coordinate: Coordinate,
    timeline: string,
    parameters: Readonly<Record<string, unknown>> = {},
  ): void {
    this.timelinePlayer.play({
      type: 'playTimeline',
      instanceId: crypto.randomUUID(),
      timeline,
      parameters: { ...parameters, position: coordinate },
      tags: [],
    });
  }

  update(deltaMs: number): Record<string, number> {
    this.timelinePlayer.update(deltaMs, this.rendererOptions.particles);
    return this.pipeline.update(deltaMs, this.rendererOptions);
  }

  setDebugOption(option: keyof RendererDebugOptions, enabled: boolean): void {
    this.rendererOptions = { ...this.rendererOptions, [option]: enabled };
    if (option === 'culling') {
      this.tilemapLayer.setCullingEnabled(enabled);
    }
    if (option === 'lighting') {
      this.entityLayer.setLightingEnabled(enabled);
    }
    this.debugState.update({ rendererOptions: this.rendererOptions });
  }

  handlePacket(packet: GamePacket): void {
    switch (packet.type) {
      case 'setCameraPosition':
        this.camera.setCoordinate(packet.coordinate);
        this.drawCameraScene();
        break;
      case 'setCameraFollowEntity':
        this.camera.followEntity(packet.networkId);
        this.drawCameraScene();
        break;
      case 'mapChunk':
        this.tilemapLayer.setMapChunk(packet);
        break;
      case 'removeMapChunk':
        this.tilemapLayer.removeMapChunk(packet);
        break;
      case 'updateMapTiles':
        this.tilemapLayer.updateMapTiles(packet);
        break;
      case 'entity':
        this.entityLayer.upsertServerEntity(packet);
        this.drawCameraScene();
        break;
      case 'moveEntity':
        this.entityLayer.moveServerEntity(packet);
        this.drawCameraScene();
        break;
      case 'turnEntity':
        this.entityLayer.turnServerEntity(packet.networkId, packet.facing);
        this.drawCameraScene();
        break;
      case 'removeEntity':
        this.entityLayer.removeServerEntity(packet.networkId);
        this.drawCameraScene();
        break;
      case 'setEnvironmentLight':
        this.lighting.setAmbientLight(packet);
        break;
      case 'playTimeline':
        this.timelinePlayer.play(packet);
        break;
      case 'stopTimeline':
        this.timelinePlayer.stop(packet);
        break;
      default:
        break;
    }
  }

  drawCameraScene(): void {
    this.scene.updateCamera(this.entityLayer);
    this.viewport.updateCameraClip();
  }

  upsertClientEntity(snapshot: ClientEntitySnapshot): void {
    this.entityLayer.upsertClientEntity(snapshot);
    this.drawCameraScene();
  }

  setClientEntityAlpha(id: number, alpha: number): void {
    this.entityLayer.setClientEntityAlpha(id, alpha);
  }

  removeClientEntity(id: number): void {
    this.entityLayer.removeClientEntity(id);
    this.drawCameraScene();
  }

  setWindowAspectRatio(width: number, height: number): void {
    this.viewport.setAspectRatio(width, height);
  }

  clearWindowAspectRatio(): void {
    this.viewport.clearAspectRatio();
  }

  setOffscreenRendering(width: number, height: number): void {
    this.viewport.setLogicalRenderSize(width, height);
  }

  setNativeRendering(): void {
    this.viewport.clearLogicalRenderSize();
  }

  setFitToScreen(enabled: boolean): void {
    this.viewport.setFitToScreen(enabled);
  }

  screenToLogical(screenX: number, screenY: number): { x: number; y: number } {
    return this.viewport.screenToLogical(screenX, screenY);
  }

  projectCoordinate(coordinate: Coordinate): { x: number; y: number } {
    const projected = projectWorldCoordinate(coordinate);
    return {
      x: this.scene.container.x + projected.x,
      y: this.scene.container.y + projected.y,
    };
  }

  projectEntity(networkId: number): { x: number; y: number } | null {
    return this.entityLayer.projectEntity(networkId);
  }

  getMapTiles(coordinate?: Coordinate, width?: number, height?: number): ClientMapTile[] {
    return this.tilemapLayer.getMapTiles(coordinate, width, height);
  }

  addMapChangedListener(listener: (coordinate: Coordinate, width: number, height: number) => void): () => void {
    return this.tilemapLayer.addMapChangedListener(listener);
  }

  setAmbientLight(color: LightColor): void { this.lighting.setAmbientLight(color); }
  getAmbientLight(): LightColor { return this.lighting.getAmbientLight(); }
  setTileLight(coordinate: Coordinate, light: TileLight | null): void { this.lighting.setTileLight(coordinate, light); }
  clearTileLights(): void { this.lighting.clearTileLights(); }

}
