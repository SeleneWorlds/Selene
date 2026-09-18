<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { createDebugState, type DebugSnapshot } from '@/core/DebugState';
import { bootstrapClient, type WebClientRuntime } from '@/core/WebClientRuntime';
import DebugOverlay from './components/DebugOverlay.vue';
import SettingsModal from './components/SettingsModal.vue';
import { loadSettings, saveSettings } from './settings';

const viewportEl = ref<HTMLDivElement | null>(null);
const bundleUiEl = ref<HTMLElement | null>(null);
const status = ref<'booting' | 'running' | 'failed'>('booting');
const errorMessage = ref<string | null>(null);
const settings = loadSettings();
const isDebugOverlayVisible = ref(settings.debugOverlayVisible);
const fitToScreen = ref(settings.fitToScreen);
const isSettingsOpen = ref(false);
const debugState = createDebugState();

let clientRuntime: WebClientRuntime | null = null;

const debugSnapshot = computed<DebugSnapshot>(() => debugState.snapshot.value);

function setDebugOverlayVisible(visible: boolean): void {
  isDebugOverlayVisible.value = visible;
  persistSettings();
}

function setFitToScreen(enabled: boolean): void {
  fitToScreen.value = enabled;
  clientRuntime?.setFitToScreen(enabled);
  persistSettings();
}

function persistSettings(): void {
  saveSettings({
    debugOverlayVisible: isDebugOverlayVisible.value,
    fitToScreen: fitToScreen.value,
  });
}

function handleGlobalKeydown(event: KeyboardEvent): void {
  if (event.repeat) {
    return;
  }

  if (event.key === 'F3') {
    event.preventDefault();
    setDebugOverlayVisible(!isDebugOverlayVisible.value);
    return;
  }

  if (event.ctrlKey && event.key === ',') {
    event.preventDefault();
    isSettingsOpen.value = !isSettingsOpen.value;
    return;
  }

  if (event.key === 'Escape' && isSettingsOpen.value) {
    event.preventDefault();
    isSettingsOpen.value = false;
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown, true);
});

onMounted(async () => {
  if (!viewportEl.value || !bundleUiEl.value) {
    status.value = 'failed';
    errorMessage.value = 'Viewport elements were not mounted.';
    return;
  }

  try {
    clientRuntime = await bootstrapClient({
      viewportHost: viewportEl.value,
      bundleUiHost: bundleUiEl.value,
      fitToScreen: fitToScreen.value,
      debugState,
    });
    status.value = 'running';
  } catch (error) {
    status.value = 'failed';
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
});

</script>

<template>
  <main class="client-shell">
    <section ref="viewportEl" class="game-viewport" aria-label="Selene game renderer" />
    <section ref="bundleUiEl" class="bundle-ui-layer" aria-label="Bundle user interface" />

    <DebugOverlay
      v-if="isDebugOverlayVisible"
      :status="status"
      :error-message="errorMessage"
      :snapshot="debugSnapshot"
      @renderer-option-changed="(option, enabled) => clientRuntime?.setRendererDebugOption(option, enabled)"
    />

    <SettingsModal
      v-if="isSettingsOpen"
      :debug-overlay-visible="isDebugOverlayVisible"
      :fit-to-screen="fitToScreen"
      @close="isSettingsOpen = false"
      @debug-overlay-visible-changed="setDebugOverlayVisible"
      @fit-to-screen-changed="setFitToScreen"
    />
  </main>
</template>
