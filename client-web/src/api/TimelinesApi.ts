import type { Coordinate } from '@/networking/GameProtocol';

/** Time is in seconds. Reusing an active instanceId updates its parameters without restarting events. */
export interface TimelinePlaybackOptions {
  instanceId?: string;
  transition?: number;
}

export interface TimelinesApi {
  playAt(coordinate: Coordinate, timeline: string, parameters?: Readonly<Record<string, unknown>>,
    options?: TimelinePlaybackOptions): void;
}
