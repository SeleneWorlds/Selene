<script setup lang="ts">
import { ref } from 'vue';
import BaseModal from './BaseModal.vue';
import ModsPanel from './ModsPanel.vue';
import SettingsModal from './SettingsModal.vue';

defineProps<{
  debugOverlayVisible: boolean;
  fitToScreen: boolean;
}>();

const page = ref<'menu' | 'settings' | 'mods'>('menu');
const emit = defineEmits<{
  close: [];
  debugOverlayVisibleChanged: [visible: boolean];
  fitToScreenChanged: [enabled: boolean];
}>();
</script>

<template>
  <SettingsModal
    v-if="page === 'settings'"
    :debug-overlay-visible="debugOverlayVisible"
    :fit-to-screen="fitToScreen"
    @back="page = 'menu'"
    @close="emit('close')"
    @debug-overlay-visible-changed="emit('debugOverlayVisibleChanged', $event)"
    @fit-to-screen-changed="emit('fitToScreenChanged', $event)"
  />
  <BaseModal
    v-else
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
    </div>
    <ModsPanel v-else />
  </BaseModal>
</template>
