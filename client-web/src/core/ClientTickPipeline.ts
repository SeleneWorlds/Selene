import type { ClientGamePreTickListener } from '@/api/GameApi';
import type { BrowserInputManager } from '@/core/BrowserInputManager';
import type { ClientMovementGrid } from '@/core/ClientMovementGrid';
import { GameLoop } from '@/core/GameLoop';
import type { GameRenderer } from '@/core/GameRenderer';

export class ClientTickPipeline {
  private readonly loop: GameLoop;
  private readonly preTickListeners = new Set<ClientGamePreTickListener>();

  constructor(
    private readonly inputManager: BrowserInputManager,
    private readonly movementGrid: ClientMovementGrid,
    private readonly renderer: GameRenderer,
  ) {
    this.loop = new GameLoop((deltaMs, elapsedMs) => this.tick(deltaMs, elapsedMs));
  }

  start(): void {
    this.loop.start();
  }

  addPreTickListener(listener: ClientGamePreTickListener): () => void {
    this.preTickListeners.add(listener);
    return () => this.preTickListeners.delete(listener);
  }

  private tick(deltaMs: number, elapsedMs: number): void {
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
    this.renderer.update(deltaMs, elapsedMs);
  }
}
