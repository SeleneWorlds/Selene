import type { Application } from 'pixi.js';
import type { DebugState, RendererDebugOptions } from '@/core/DebugState';
import type { PixiEntityLayer } from '@/renderer/entities/PixiEntityLayer';
import type { InteriorFadeController } from '@/renderer/InteriorFadeController';
import type { PixiScene } from '@/renderer/PixiScene';
import type { PixiViewport } from '@/renderer/PixiViewport';
import type { PixiTilemapLayer } from '@/renderer/tiles/PixiTilemapLayer';

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
  ) {}

  update(deltaMs: number, options: RendererDebugOptions): Record<string, number> {
    this.recordFrame(deltaMs);
    const timings: Record<string, number> = {};
    this.measure(timings, 'Renderer: entities', () => {
      if (options.entityUpdates) this.entityLayer.update(deltaMs);
    });
    this.measure(timings, 'Renderer: camera', () => this.scene.updateCamera(this.entityLayer));
    const focus = this.measure(timings, 'Renderer: focus bounds', () => this.scene.getFocus(this.entityLayer));
    this.measure(timings, 'Renderer: interior fade', () => {
      if (options.interiorFade) this.interiorFade.update(deltaMs, focus.coordinate);
    });
    this.measure(timings, 'Renderer: tile animations', () => {
      if (options.tileAnimations) this.tilemapLayer.updateAnimations(deltaMs);
    });
    this.measure(timings, 'Renderer: tile occlusion', () => {
      if (options.tileOcclusion) this.tilemapLayer.updateOcclusion(deltaMs, focus.coordinate, focus.bounds);
    });
    this.measure(timings, 'Renderer: scene update', () => this.viewport.updateCameraClip());
    this.measure(timings, 'Renderer: tile cull', () => {
      if (options.pixiRender && options.culling) {
        this.tilemapLayer.updateCulling(this.scene.getViewBounds(
          this.app.renderer.screen.width, this.app.renderer.screen.height,
        ));
      }
    });
    this.measure(timings, 'Renderer: Pixi draw', () => {
      if (options.pixiRender) this.app.renderer.render({ container: this.app.stage });
    });
    return timings;
  }

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

  private measure<T>(timings: Record<string, number>, label: string, operation: () => T): T {
    const startedAt = performance.now();
    try {
      return operation();
    } finally {
      timings[label] = performance.now() - startedAt;
    }
  }
}
