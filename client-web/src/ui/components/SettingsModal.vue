<script setup lang="ts">
import { ref } from 'vue';
import ModsPanel from './ModsPanel.vue';

defineProps<{
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
}>();

const page = ref<'settings' | 'mods'>('settings');
const emit = defineEmits<{
  close: [];
  debugOverlayVisibleChanged: [visible: boolean];
  fitToScreenChanged: [enabled: boolean];
}>();
</script>

<template>
  <div class="settings-modal" role="presentation" @mousedown.self="emit('close')">
    <section
      class="settings-modal__dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="settings-title"
    >
      <header class="settings-modal__header">
        <button
          v-if="page === 'mods'"
          class="settings-modal__back"
          type="button"
          aria-label="Back to settings"
          @click="page = 'settings'"
        >
          ‹
        </button>
        <h1 id="settings-title">{{ page === 'mods' ? 'Mods' : 'Settings' }}</h1>
        <button class="settings-modal__close" type="button" aria-label="Close settings" @click="emit('close')">
          ×
        </button>
      </header>

      <div v-if="page === 'settings'" class="settings-modal__body">
        <label class="settings-toggle">
          <span>
            <strong>Fit to Screen</strong>
            <small>Scale the game to fit the window. Turn this off for a centered 1:1 view.</small>
          </span>
          <input
            type="checkbox"
            :checked="fitToScreen"
            @change="emit('fitToScreenChanged', ($event.target as HTMLInputElement).checked)"
          >
        </label>

        <label class="settings-toggle">
          <span>
            <strong>Debug Overlay</strong>
            <small>Show performance and renderer diagnostics. You can also toggle this with F3.</small>
          </span>
          <input
            type="checkbox"
            :checked="debugOverlayVisible"
            @change="emit('debugOverlayVisibleChanged', ($event.target as HTMLInputElement).checked)"
          >
        </label>

        <button class="settings-menu-button" type="button" @click="page = 'mods'">
          <span>
            <strong>Mods</strong>
            <small>Override game assets with files from a ZIP archive.</small>
          </span>
          <span aria-hidden="true">›</span>
        </button>
      </div>

      <ModsPanel v-else />
    </section>
  </div>
</template>
