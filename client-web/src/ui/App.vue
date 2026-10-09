<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { createDebugState, type DebugSnapshot } from '@/core/DebugState';
import { bootstrapClient, type WebClientRuntime } from '@/core/WebClientRuntime';
import DebugOverlay from './components/DebugOverlay.vue';
import ModDropInstaller from './components/ModDropInstaller.vue';
import ClientMenu from './components/ClientMenu.vue';
import StartupScreen from './components/StartupScreen.vue';
import { loadSettings, saveSettings } from './settings';

const viewportEl = ref<HTMLDivElement | null>(null);
const bundleUiEl = ref<HTMLElement | null>(null);
const status = ref<'booting' | 'running' | 'failed'>('booting');
const errorTitle = ref('Unable to connect');
const errorMessage = ref<string | null>(null);
const canRetryStartup = ref(true);
const startupLabel = ref('Starting client');
const startupProgress = ref(0);
const settings = loadSettings();
const isDebugOverlayVisible = ref(settings.debugOverlayVisible);
const fitToScreen = ref(settings.fitToScreen);
const isMenuOpen = ref(false);
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

function retryStartup(): void {
  window.location.reload();
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
    isMenuOpen.value = !isMenuOpen.value;
    return;
  }

  if (event.key === 'Escape' && isMenuOpen.value) {
    event.preventDefault();
    event.stopImmediatePropagation();
    isMenuOpen.value = false;
  }

}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown, true);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKeydown, true);
});

async function startClient(): Promise<void> {
  if (!viewportEl.value || !bundleUiEl.value) {
    status.value = 'failed';
    errorMessage.value = 'Viewport elements were not mounted.';
    return;
  }

  status.value = 'booting';
  errorTitle.value = 'Unable to connect';
  errorMessage.value = null;
  canRetryStartup.value = true;
  startupLabel.value = 'Starting client';
  startupProgress.value = 0;

  try {
    clientRuntime = await bootstrapClient({
      viewportHost: viewportEl.value,
      bundleUiHost: bundleUiEl.value,
      fitToScreen: fitToScreen.value,
      debugState,
      onStartupProgress: ({ label, progress }) => {
        startupLabel.value = label;
        startupProgress.value = progress;
      },
      onDisconnected: (reason) => {
        status.value = 'failed';
        errorTitle.value = 'Disconnected';
        errorMessage.value = reason || 'The server disconnected the client.';
      },
    });
    if (status.value === 'booting') {
      status.value = 'running';
    }
  } catch (error) {
    status.value = 'failed';
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

onMounted(() => {
  void startClient();
});

</script>

<template>
  <main class="client-shell">
    <section ref="viewportEl" class="game-viewport" aria-label="Selene game renderer" />
    <section ref="bundleUiEl" class="bundle-ui-layer" aria-label="Bundle user interface" />

    <StartupScreen
      v-if="status !== 'running'"
      :status="status"
      :label="startupLabel"
      :progress="startupProgress"
      :error-title="errorTitle"
      :error-message="errorMessage"
      :can-retry="canRetryStartup"
      @retry="retryStartup"
    />

    <DebugOverlay
      v-if="isDebugOverlayVisible"
      :status="status"
      :error-message="errorMessage"
      :snapshot="debugSnapshot"
      @renderer-option-changed="(option, enabled) => clientRuntime?.setRendererDebugOption(option, enabled)"
    />

    <ClientMenu
      v-if="isMenuOpen"
      :debug-overlay-visible="isDebugOverlayVisible"
      :fit-to-screen="fitToScreen"
      @close="isMenuOpen = false"
      @debug-overlay-visible-changed="setDebugOverlayVisible"
      @fit-to-screen-changed="setFitToScreen"
    />

    <ModDropInstaller />
  </main>
</template>
