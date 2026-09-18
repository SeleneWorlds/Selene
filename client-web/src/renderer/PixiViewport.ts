import type { Application } from 'pixi.js';
import type { DebugState } from '@/core/DebugState';
import type { ClientCamera } from '@/core/ClientCamera';

export const RENDER_RESOLUTION = 1;

export class PixiViewport {
  private readonly resizeObserver: ResizeObserver;
  private aspectRatio: { width: number; height: number } | null = null;
  private logicalRenderSize: { width: number; height: number } | null = null;
  private fitToScreen = true;
  private renderScale = 1;
  private renderOffsetX = 0;
  private renderOffsetY = 0;
  private renderWidth = 1;
  private renderHeight = 1;
  private initialized = false;

  constructor(
    private readonly app: Application,
    private readonly host: HTMLElement,
    private readonly uiHost: HTMLElement,
    private readonly camera: ClientCamera,
    private readonly debugState: DebugState,
    private readonly sceneChanged: () => void,
  ) {
    this.resizeObserver = new ResizeObserver(() => this.resize());
  }

  initialize(): void {
    this.initialized = true;
    this.resizeObserver.observe(this.host);
    this.resize();
  }

  setAspectRatio(width: number, height: number): void {
    assertPositiveSize(width, height, 'Window aspect ratio');
    this.aspectRatio = { width, height };
    this.resize();
  }

  clearAspectRatio(): void {
    this.aspectRatio = null;
    this.resize();
  }

  setLogicalRenderSize(width: number, height: number): void {
    assertPositiveSize(width, height, 'Offscreen rendering');
    this.logicalRenderSize = { width, height };
    this.resize();
  }

  clearLogicalRenderSize(): void {
    this.logicalRenderSize = null;
    this.resize();
  }

  setFitToScreen(enabled: boolean): void {
    this.fitToScreen = enabled;
    this.resize();
  }

  screenToLogical(screenX: number, screenY: number): { x: number; y: number } {
    return {
      x: (screenX - this.renderOffsetX) / this.renderScale,
      y: (screenY - this.renderOffsetY) / this.renderScale,
    };
  }

  updateCameraClip(): void {
    if (!this.initialized) {
      return;
    }
    const viewport = this.camera.getViewportRect();
    const clipTop = viewport.y / this.renderHeight * 100;
    const clipRight = (this.renderWidth - viewport.x - viewport.width) / this.renderWidth * 100;
    const clipBottom = (this.renderHeight - viewport.y - viewport.height) / this.renderHeight * 100;
    const clipLeft = viewport.x / this.renderWidth * 100;
    this.app.canvas.style.clipPath = `inset(${clipTop}% ${clipRight}% ${clipBottom}% ${clipLeft}%)`;
  }

  private resize(): void {
    if (!this.initialized) {
      return;
    }
    const hostWidth = Math.max(1, this.host.clientWidth);
    const hostHeight = Math.max(1, this.host.clientHeight);
    const targetAspect = this.aspectRatio
      ? this.aspectRatio.width / this.aspectRatio.height
      : hostWidth / hostHeight;
    const hostAspect = hostWidth / hostHeight;
    const fittedWidth = hostAspect > targetAspect ? hostHeight * targetAspect : hostWidth;
    const fittedHeight = hostAspect > targetAspect ? hostHeight : hostWidth / targetAspect;
    const width = this.logicalRenderSize?.width ?? Math.max(1, Math.round(fittedWidth));
    const height = this.logicalRenderSize?.height ?? Math.max(1, Math.round(fittedHeight));
    const screenWidth = this.fitToScreen ? fittedWidth : width;
    const screenHeight = this.fitToScreen ? fittedHeight : height;

    this.renderWidth = width;
    this.renderHeight = height;
    this.renderOffsetX = (hostWidth - screenWidth) / 2;
    this.renderOffsetY = (hostHeight - screenHeight) / 2;
    this.renderScale = screenWidth / width;
    this.camera.setViewport(width, height);
    this.app.renderer.resize(width, height, RENDER_RESOLUTION);
    Object.assign(this.app.canvas.style, {
      position: 'absolute', left: `${this.renderOffsetX}px`, top: `${this.renderOffsetY}px`,
      width: `${screenWidth}px`, height: `${screenHeight}px`,
    });
    Object.assign(this.uiHost.style, {
      left: `${this.renderOffsetX}px`, top: `${this.renderOffsetY}px`, width: `${width}px`, height: `${height}px`,
      transform: `scale(${this.renderScale})`, transformOrigin: 'top left',
    });
    this.sceneChanged();
    this.debugState.update({ viewport: { width, height, resolution: RENDER_RESOLUTION } });
  }
}

function assertPositiveSize(width: number, height: number, label: string): void {
  if (width <= 0 || height <= 0) {
    throw new Error(`${label} dimensions must be positive.`);
  }
}
