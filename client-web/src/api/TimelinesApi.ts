import type { Coordinate } from '@/networking/GameProtocol';

export interface TimelinesApi {
  playAt(coordinate: Coordinate, timeline: string, parameters?: Readonly<Record<string, unknown>>): void;
}
