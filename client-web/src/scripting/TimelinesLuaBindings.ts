import type { TimelinesApi, TimelinePlaybackOptions } from '@/api/TimelinesApi';
import { LuaArguments } from './LuaArguments';
import type { LuaRuntime } from './LuaRuntime';

export async function registerTimelinesLuaModule(runtime: LuaRuntime, timelines: TimelinesApi): Promise<void> {
  const playAt = (coordinate: unknown, timeline: unknown, parameters?: unknown, options?: unknown) => {
    const args = new LuaArguments('selene.timelines.playAt');
    const playbackOptions = readOptions(args, options);

    timelines.playAt(
      args.coordinate(coordinate, 'coordinate'),
      args.string(timeline, 'timeline'),
      parameters === undefined || parameters === null
        ? undefined
        : args.serializedMap(parameters, 'parameters'),
      playbackOptions,
    );
  };

  await runtime.preloadModule('selene.timelines', () => ({ playAt }));
}

function readOptions(args: LuaArguments, value: unknown): TimelinePlaybackOptions {
  if (value === undefined || value === null) return {};
  const values = args.serializedMap(value, 'options');
  const options: TimelinePlaybackOptions = {};
  if (values.instanceId !== undefined && values.instanceId !== null) {
    options.instanceId = args.string(values.instanceId, 'options.instanceId');
    if (!options.instanceId.trim()) throw new Error('instanceId must not be blank');
  }
  if (values.transition !== undefined && values.transition !== null) {
    if (typeof values.transition !== 'number' || !Number.isFinite(values.transition) || values.transition < 0) {
      throw new Error('transition must be a non-negative finite number');
    }
    options.transition = values.transition;
  }
  return options;
}
