<script setup lang="ts">
defineProps<{
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
}>();

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
        <h1 id="settings-title">Settings</h1>
        <button class="settings-modal__close" type="button" aria-label="Close settings" @click="emit('close')">
          ×
        </button>
      </header>

      <div class="settings-modal__body">
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
      </div>
    </section>
  </div>
</template>
