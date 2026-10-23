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
  rendererOptions: RendererDebugOptions;
}

export interface RendererDebugOptions {
  cameraEvents: boolean;
  culling: boolean;
  entityUpdates: boolean;
  lighting: boolean;
  particles: boolean;
  tileAnimations: boolean;
  smoothLighting: boolean;
  tileOcclusion: boolean;
  interiorFade: boolean;
  pixiRender: boolean;
}

export interface DebugState {
  snapshot: ShallowRef<DebugSnapshot>;
  update: (next: PartialDebugSnapshot) => void;
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
  rendererOptions: {
    cameraEvents: true,
    culling: true,
    entityUpdates: true,
    lighting: true,
    particles: true,
    tileAnimations: true,
    smoothLighting: false,
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
  };
}
