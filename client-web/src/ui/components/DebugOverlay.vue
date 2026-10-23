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

const rendererOptionLabels: Record<Exclude<keyof RendererDebugOptions, 'cameraEvents' | 'entityUpdates' | 'pixiRender' | 'tileAnimations' | 'smoothLighting'>, string> = {
  culling: 'Tile Culling',
  lighting: 'Lighting',
  particles: 'Particles',
  tileOcclusion: 'Tile Occlusion',
  interiorFade: 'Interior Fade',
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
        <dt>Delta</dt>
        <dd>{{ snapshot.deltaMs.toFixed(1) }} ms</dd>
      </div>
    </dl>

    <section class="debug-renderer" aria-label="Debug Toggles">
      <details open>
        <summary>Debug Toggles</summary>
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
