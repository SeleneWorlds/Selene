import type { TimelinesApi } from '@/api/TimelinesApi';
import { LuaArguments } from './LuaArguments';
import type { LuaRuntime } from './LuaRuntime';

export async function registerTimelinesLuaModule(runtime: LuaRuntime, timelines: TimelinesApi): Promise<void> {
  const playAt = (coordinate: unknown, timeline: unknown, parameters?: unknown) => {
    const args = new LuaArguments('selene.timelines.playAt');

    timelines.playAt(
      args.coordinate(coordinate, 'coordinate'),
      args.string(timeline, 'timeline'),
      parameters === undefined || parameters === null
        ? undefined
        : args.serializedMap(parameters, 'parameters'),
    );
  };

  await runtime.preloadModule('selene.timelines', () => ({ playAt }));
}
