<script setup lang="ts">
import { ref } from 'vue';
import BaseModal from './BaseModal.vue';
import ModsPanel from './ModsPanel.vue';
import SettingsModal from './SettingsModal.vue';
import ControlsModal from './ControlsModal.vue';

defineProps<{
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
  tileAnimations: boolean;
  smoothLighting: boolean;
}>();

const page = ref<'menu' | 'settings' | 'mods' | 'controls'>('menu');
const controlsModal = ref<InstanceType<typeof ControlsModal> | null>(null);
defineExpose({ handleKeydown: (event: KeyboardEvent) => controlsModal.value?.handleKeydown(event) ?? false });
const emit = defineEmits<{
  close: [];
  debugOverlayVisibleChanged: [visible: boolean];
  fitToScreenChanged: [enabled: boolean];
  tileAnimationsChanged: [enabled: boolean];
  smoothLightingChanged: [enabled: boolean];
}>();
</script>

<template>
  <SettingsModal
    v-if="page === 'settings'"
    :debug-overlay-visible="debugOverlayVisible"
    :fit-to-screen="fitToScreen"
    :tile-animations="tileAnimations"
    :smooth-lighting="smoothLighting"
    @back="page = 'menu'"
    @close="emit('close')"
    @debug-overlay-visible-changed="emit('debugOverlayVisibleChanged', $event)"
    @fit-to-screen-changed="emit('fitToScreenChanged', $event)"
    @smooth-lighting-changed="emit('smoothLightingChanged', $event)"
    @tile-animations-changed="emit('tileAnimationsChanged', $event)"
  />
  <BaseModal
    v-else-if="page !== 'controls'"
    :title="page === 'mods' ? 'Mods' : 'Menu'"
    :show-back="page === 'mods'"
    @back="page = 'menu'"
    @close="emit('close')"
  >
    <div v-if="page === 'menu'" class="settings-modal__body">
      <button class="settings-menu-button" type="button" @click="page = 'settings'">
        <span><strong>Settings</strong><small>Configure display and debug options.</small></span>
        <span aria-hidden="true">›</span>
      </button>
      <button class="settings-menu-button" type="button" @click="page = 'mods'">
        <span><strong>Mods</strong><small>Override game assets with files from a ZIP archive.</small></span>
        <span aria-hidden="true">›</span>
      </button>
      <button class="settings-menu-button" type="button" @click="page = 'controls'">
        <span><strong>Controls</strong><small>Rebind or unbind keyboard shortcuts.</small></span>
        <span aria-hidden="true">›</span>
      </button>
    </div>
    <ModsPanel v-else />
  </BaseModal>
  <ControlsModal v-else ref="controlsModal" @back="page = 'menu'" @close="emit('close')" />
</template>
