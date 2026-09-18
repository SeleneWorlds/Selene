import { LuaArguments } from './LuaArguments';

export type LuaCallback = (...args: unknown[]) => void;

export function createSignal(
  context: string,
  connectListener: (callback: LuaCallback) => () => void,
): { connect: (callback: unknown) => void } {
  return {
    connect(callback: unknown): void {
      const args = new LuaArguments(`${context}.connect`);

      connectListener(args.function(callback, 'callback'));
    },
  };
}
