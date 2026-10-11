import type { PixiTileGrid } from '@/renderer/PixiTileGrid';
import type { Application } from 'pixi.js';
import type { DebugState, RendererDebugOptions } from '@/core/DebugState';
import type { PixiEntityLayer } from '@/renderer/entities/PixiEntityLayer';
import { isInsideInterior, type InteriorFadeController } from '@/renderer/InteriorFadeController';
import type { PixiScene } from '@/renderer/PixiScene';
import type { PixiViewport } from '@/renderer/PixiViewport';
import type { PixiTilemapLayer } from '@/renderer/tiles/PixiTilemapLayer';
import type { LightingEnvironment } from '@/renderer/LightingEnvironment';

export class RenderPipeline {
  private frame = 0;
  private fpsElapsedMs = 0;
  private fpsFrames = 0;

  constructor(
    private readonly app: Application,
    private readonly debugState: DebugState,
    private readonly scene: PixiScene,
    private readonly viewport: PixiViewport,
    private readonly entityLayer: PixiEntityLayer,
    private readonly tilemapLayer: PixiTilemapLayer,
    private readonly interiorFade: InteriorFadeController,
    private readonly lighting: LightingEnvironment,
    private readonly tileGrid: PixiTileGrid,
  ) {}

  update(deltaMs: number, options: RendererDebugOptions, elapsedMs: number): void {
    this.recordFrame(elapsedMs);
    this.tilemapLayer.processPendingWork();
    if (options.entityUpdates) this.entityLayer.update(deltaMs);

    this.scene.updateCamera(this.entityLayer);
    const focus = this.scene.getFocus(this.entityLayer);
    this.lighting.setActiveAmbientLight(Math.round(focus.coordinate.z) < 0
      ? 'underground' : isInsideInterior(this.tilemapLayer, focus.coordinate) ? 'indoors' : 'outdoors');
    if (options.lighting) {
      const revision = this.lighting.getRevision();
      if (revision !== this.lastLightingRevision) {
        this.lastLightingRevision = revision;
        this.tilemapLayer.updateLighting();
        this.entityLayer.updateLighting();
      }
    }

    if (options.interiorFade) this.interiorFade.update(deltaMs, focus.coordinate);

    if (options.tileAnimations) this.tilemapLayer.updateAnimations(deltaMs);

    if (options.tileOcclusion) this.tilemapLayer.updateOcclusion(deltaMs, focus.coordinate, focus.bounds);

    this.viewport.updateCameraClip();
    if (options.pixiRender && options.culling) {
      this.tilemapLayer.updateCulling(this.scene.getViewBounds(
        this.app.renderer.screen.width, this.app.renderer.screen.height,
      ));
    }

    this.tileGrid.update(this.scene.getViewBounds(
      this.app.renderer.screen.width, this.app.renderer.screen.height,
    ), focus.coordinate.z);
    if (options.pixiRender) this.app.renderer.render({ container: this.app.stage });
  }

  private lastLightingRevision = -1;

  private recordFrame(deltaMs: number): void {
    this.frame += 1;
    this.fpsElapsedMs += deltaMs;
    this.fpsFrames += 1;
    if (this.fpsElapsedMs >= 250) {
      this.debugState.update({ frame: this.frame, fps: this.fpsFrames / this.fpsElapsedMs * 1000, deltaMs });
      this.fpsElapsedMs = 0;
      this.fpsFrames = 0;
    }
  }
}
