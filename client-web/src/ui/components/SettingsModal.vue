<script setup lang="ts">
import BaseModal from './BaseModal.vue';

defineProps<{
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
}>();

const emit = defineEmits<{
  back: [];
  close: [];
  debugOverlayVisibleChanged: [visible: boolean];
  fitToScreenChanged: [enabled: boolean];
}>();
</script>

<template>
  <BaseModal title="Settings" show-back @back="emit('back')" @close="emit('close')">
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
  </BaseModal>
</template>
