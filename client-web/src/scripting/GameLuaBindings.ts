import type { GameApi } from '@/api/GameApi';
import { LuaArguments } from './LuaArguments';
import { createSignal } from './LuaSignal';
import type { LuaRuntime } from './LuaRuntime';

export async function registerGameLuaModule(runtime: LuaRuntime, game: GameApi): Promise<void> {
  const preTick = createSignal('selene.game.preTick', (callback) => game.addPreTickListener(callback));
  const setWindowAspectRatio = (width: unknown, height: unknown) => {
    const args = new LuaArguments('selene.game.setWindowAspectRatio');

    game.setWindowAspectRatio(
      args.integer(width, 'width'),
      args.integer(height, 'height'),
    );
  };
  const clearWindowAspectRatio = () => {
    game.clearWindowAspectRatio();
  };
  const setOffscreenRendering = (width: unknown, height: unknown) => {
    const args = new LuaArguments('selene.game.setOffscreenRendering');

    game.setOffscreenRendering(
      args.integer(width, 'width'),
      args.integer(height, 'height'),
    );
  };
  const setNativeRendering = () => {
    game.setNativeRendering();
  };

  await runtime.preloadModule('selene.game', () => ({
    preTick,
    setWindowAspectRatio,
    clearWindowAspectRatio,
    setOffscreenRendering,
    setNativeRendering,
  }));
}
