import type { ClientGamePreTickListener } from '@/api/GameApi';
import type { ClientEntityScriptRunner } from '@/api/EntitiesApi';
import type { BrowserInputManager } from '@/core/BrowserInputManager';
import type { ClientEntities } from '@/core/ClientEntities';
import type { ClientMovementGrid } from '@/core/ClientMovementGrid';
import { GameLoop } from '@/core/GameLoop';
import type { GameRenderer } from '@/core/GameRenderer';

export class ClientTickPipeline {
  private readonly loop: GameLoop;
  private readonly preTickListeners = new Set<ClientGamePreTickListener>();
  private clientEntityScriptRunner: ClientEntityScriptRunner | null = null;

  constructor(
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
    for (const listener of [...this.preTickListeners]) {
      try {
        listener();
      } catch (error) {
        console.error('[Lua] Error in selene.game.preTick listener', error);
      }
    }

    this.inputManager.update();
    try {
      this.movementGrid.update(deltaMs);
    } catch (error) {
      console.error('[Lua] Error in selene.movement.grid update', error);
    }
    if (this.clientEntityScriptRunner) {
      try {
        this.entities.tickClientScripts(deltaMs / 1000, this.clientEntityScriptRunner);
      } catch (error) {
        console.error('[Lua] Error in client entity script', error);
      }
    }
    this.renderer.update(deltaMs);
  }
}
