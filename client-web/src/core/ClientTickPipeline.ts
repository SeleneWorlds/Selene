import type { ClientGamePreTickListener } from '@/api/GameApi';
import type { ClientEntityScriptRunner } from '@/api/EntitiesApi';
import type { BrowserInputManager } from '@/core/BrowserInputManager';
import type { ClientEntities } from '@/core/ClientEntities';
import type { ClientMovementGrid } from '@/core/ClientMovementGrid';
import type { DebugState } from '@/core/DebugState';
import { GameLoop } from '@/core/GameLoop';
import type { GameRenderer } from '@/core/GameRenderer';

export class ClientTickPipeline {
  private readonly loop: GameLoop;
  private readonly preTickListeners = new Set<ClientGamePreTickListener>();
  private clientEntityScriptRunner: ClientEntityScriptRunner | null = null;
  private frameTimingElapsedMs = 0;
  private frameTimingPeakMs = 0;
  private frameTimingSamples = 0;
  private readonly frameTimingTotals: Record<string, number> = {};

  constructor(
    private readonly debugState: DebugState,
    private readonly inputManager: BrowserInputManager,
    private readonly movementGrid: ClientMovementGrid,
    private readonly entities: ClientEntities,
    private readonly renderer: GameRenderer,
  ) {
    this.loop = new GameLoop((deltaMs) => this.tick(deltaMs));
  }

  start(): void {
    this.loop.start();
  }

  addPreTickListener(listener: ClientGamePreTickListener): () => void {
    this.preTickListeners.add(listener);
    return () => this.preTickListeners.delete(listener);
  }

  setClientEntityScriptRunner(runner: ClientEntityScriptRunner): void {
    this.clientEntityScriptRunner = runner;
  }

  private tick(deltaMs: number): void {
    const frameStartedAt = performance.now();
    let sectionStartedAt = frameStartedAt;
    for (const listener of [...this.preTickListeners]) {
      try {
        listener();
      } catch (error) {
        console.error('[Lua] Error in selene.game.preTick listener', error);
      }
    }
    this.addFrameTiming('Lua pre-tick', performance.now() - sectionStartedAt);

    sectionStartedAt = performance.now();
    this.inputManager.update();
    this.addFrameTiming('Input', performance.now() - sectionStartedAt);
    sectionStartedAt = performance.now();
    try {
      this.movementGrid.update(deltaMs);
    } catch (error) {
      console.error('[Lua] Error in selene.movement.grid update', error);
    }
    this.addFrameTiming('Movement', performance.now() - sectionStartedAt);
    sectionStartedAt = performance.now();
    if (this.clientEntityScriptRunner) {
      try {
        this.entities.tickClientScripts(deltaMs / 1000, this.clientEntityScriptRunner);
      } catch (error) {
        console.error('[Lua] Error in client entity script', error);
      }
    }
    this.addFrameTiming('Entity scripts', performance.now() - sectionStartedAt);
    sectionStartedAt = performance.now();
    const rendererTimings = this.renderer.update(deltaMs);
    this.addFrameTiming('Renderer', performance.now() - sectionStartedAt);
    for (const [label, durationMs] of Object.entries(rendererTimings)) {
      this.addFrameTiming(label, durationMs);
    }
    this.finishFrameTiming(performance.now() - frameStartedAt);
  }

  private addFrameTiming(label: string, durationMs: number): void {
    this.frameTimingTotals[label] = (this.frameTimingTotals[label] ?? 0) + durationMs;
  }

  private finishFrameTiming(totalMs: number): void {
    this.frameTimingElapsedMs += totalMs;
    this.frameTimingPeakMs = Math.max(this.frameTimingPeakMs, totalMs);
    this.frameTimingSamples += 1;

    if (this.frameTimingSamples < 30) {
      return;
    }

    const samples = this.frameTimingSamples;
    this.debugState.update({
      frameTiming: {
        averageMs: this.frameTimingElapsedMs / samples,
        peakMs: this.frameTimingPeakMs,
        samples,
        breakdown: Object.fromEntries(
          Object.entries(this.frameTimingTotals).map(([label, duration]) => [label, duration / samples]),
        ),
      },
    });
    this.frameTimingElapsedMs = 0;
    this.frameTimingPeakMs = 0;
    this.frameTimingSamples = 0;
    for (const label of Object.keys(this.frameTimingTotals)) {
      delete this.frameTimingTotals[label];
    }
  }
}
