<script setup lang="ts">
import type { DebugSnapshot, RendererDebugOptions } from '@/core/DebugState';

defineProps<{
  status: 'booting' | 'running' | 'failed';
  errorMessage: string | null;
  snapshot: DebugSnapshot;
}>();

const emit = defineEmits<{
  rendererOptionChanged: [option: keyof RendererDebugOptions, enabled: boolean];
}>();

const rendererOptionLabels: Record<keyof RendererDebugOptions, string> = {
  culling: 'Tile culling',
  entityUpdates: 'Entity updates',
  tileAnimations: 'Tile animations',
  tileOcclusion: 'Tile occlusion',
  interiorFade: 'Interior fade',
  pixiRender: 'Pixi render',
};
</script>

<template>
  <aside class="debug-overlay" aria-label="Debug overlay">
    <div class="debug-overlay__header">
      <span class="debug-overlay__title">Selene Web Client</span>
      <span class="debug-overlay__status" :data-status="status">{{ status }}</span>
    </div>

    <dl class="debug-grid">
      <div>
        <dt>FPS</dt>
        <dd>{{ snapshot.fps.toFixed(0) }}</dd>
      </div>
      <div>
        <dt>Frame</dt>
        <dd>{{ snapshot.frame }}</dd>
      </div>
      <div>
        <dt>Delta</dt>
        <dd>{{ snapshot.deltaMs.toFixed(1) }} ms</dd>
      </div>
      <div>
        <dt>Canvas</dt>
        <dd>{{ snapshot.viewport.width }} x {{ snapshot.viewport.height }}</dd>
      </div>
      <div>
        <dt>Network</dt>
        <dd>{{ snapshot.networkStatus }}</dd>
      </div>
      <div>
        <dt>Renderer</dt>
        <dd>{{ snapshot.rendererStatus }}</dd>
      </div>
    </dl>

    <section class="debug-timings" aria-label="Performance timings">
      <h2>Performance</h2>
      <div class="debug-timings__summary">
        <span>Startup</span>
        <strong>{{ snapshot.loadTotalMs > 0 ? `${snapshot.loadTotalMs.toFixed(0)} ms` : 'loading…' }}</strong>
        <span>Frame work avg / peak</span>
        <strong>{{ snapshot.frameTiming.averageMs.toFixed(2) }} / {{ snapshot.frameTiming.peakMs.toFixed(2) }} ms</strong>
      </div>

      <details open>
        <summary>Loading phases</summary>
        <dl class="debug-timing-list">
          <div v-for="timing in snapshot.loadTimings" :key="timing.label">
            <dt>{{ timing.label }}</dt>
            <dd>{{ timing.durationMs.toFixed(1) }} ms</dd>
          </div>
        </dl>
      </details>

      <details v-if="snapshot.frameTiming.samples > 0">
        <summary>Frame breakdown ({{ snapshot.frameTiming.samples }} samples)</summary>
        <dl class="debug-timing-list">
          <div v-for="(duration, label) in snapshot.frameTiming.breakdown" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ duration.toFixed(2) }} ms</dd>
          </div>
        </dl>
      </details>

      <details open>
        <summary>Renderer switches</summary>
        <div class="debug-renderer-options">
          <label v-for="(label, option) in rendererOptionLabels" :key="option">
            <input
              type="checkbox"
              :checked="snapshot.rendererOptions[option]"
              @change="emit('rendererOptionChanged', option, ($event.target as HTMLInputElement).checked)"
            >
            {{ label }}
          </label>
        </div>
      </details>
    </section>

    <p v-if="errorMessage" class="debug-overlay__error">{{ errorMessage }}</p>
  </aside>
</template>
