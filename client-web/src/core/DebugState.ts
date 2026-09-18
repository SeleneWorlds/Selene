import { shallowRef, type ShallowRef } from 'vue';
import type { NetworkStatus } from '@/networking/NetworkClient';

export interface DebugSnapshot {
  frame: number;
  fps: number;
  deltaMs: number;
  viewport: {
    width: number;
    height: number;
    resolution: number;
  };
  networkStatus: NetworkStatus;
  rendererStatus: 'created' | 'initializing' | 'ready';
  loadTimings: DebugLoadTiming[];
  loadTotalMs: number;
  frameTiming: DebugFrameTiming;
  rendererOptions: RendererDebugOptions;
}

export interface RendererDebugOptions {
  culling: boolean;
  entityUpdates: boolean;
  tileAnimations: boolean;
  tileOcclusion: boolean;
  interiorFade: boolean;
  pixiRender: boolean;
}

export interface DebugLoadTiming {
  label: string;
  durationMs: number;
}

export interface DebugFrameTiming {
  averageMs: number;
  peakMs: number;
  samples: number;
  breakdown: Record<string, number>;
}

export interface DebugState {
  snapshot: ShallowRef<DebugSnapshot>;
  update: (next: PartialDebugSnapshot) => void;
  recordLoadTiming: (label: string, durationMs: number) => void;
}

type PartialDebugSnapshot = Partial<
  Omit<DebugSnapshot, 'viewport'> & {
    viewport: Partial<DebugSnapshot['viewport']>;
  }
>;

const initialSnapshot: DebugSnapshot = {
  frame: 0,
  fps: 0,
  deltaMs: 0,
  viewport: {
    width: 0,
    height: 0,
    resolution: 1,
  },
  networkStatus: 'idle',
  rendererStatus: 'created',
  loadTimings: [],
  loadTotalMs: 0,
  frameTiming: {
    averageMs: 0,
    peakMs: 0,
    samples: 0,
    breakdown: {},
  },
  rendererOptions: {
    culling: true,
    entityUpdates: true,
    tileAnimations: true,
    tileOcclusion: true,
    interiorFade: true,
    pixiRender: true,
  },
};

export function createDebugState(): DebugState {
  const snapshot = shallowRef<DebugSnapshot>(initialSnapshot);

  return {
    snapshot,
    update(next) {
      snapshot.value = {
        ...snapshot.value,
        ...next,
        viewport: {
          ...snapshot.value.viewport,
          ...next.viewport,
        },
      };
    },
    recordLoadTiming(label, durationMs) {
      const timing = { label, durationMs };
      console.info(`[Timing] ${label}: ${durationMs.toFixed(1)} ms`);
      snapshot.value = {
        ...snapshot.value,
        loadTimings: [...snapshot.value.loadTimings, timing],
      };
    },
  };
}
