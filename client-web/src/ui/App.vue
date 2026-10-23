<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { createDebugState, type DebugSnapshot } from '@/core/DebugState';
import { bootstrapClient, type WebClientRuntime } from '@/core/WebClientRuntime';
import DebugOverlay from './components/DebugOverlay.vue';
import ModDropInstaller from './components/ModDropInstaller.vue';
import ClientMenu from './components/ClientMenu.vue';
import StartupScreen from './components/StartupScreen.vue';
import { loadSettings, saveSettings } from './settings';
import { keyboardControls } from './controls';

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
const tileAnimations = ref(settings.tileAnimations);
const smoothLighting = ref(settings.smoothLighting);
const isMenuOpen = ref(false);
const clientMenu = ref<InstanceType<typeof ClientMenu> | null>(null);
const releases: Array<() => void> = [];
watch(isMenuOpen, (open) => { keyboardControls.blocked = open; }, { flush: 'sync' });
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

function setTileAnimations(enabled: boolean): void {
  tileAnimations.value = enabled;
  clientRuntime?.setRendererDebugOption('tileAnimations', enabled);
  persistSettings();
}

function setSmoothLighting(enabled: boolean): void {
  smoothLighting.value = enabled;
  clientRuntime?.setRendererDebugOption('smoothLighting', enabled);
  persistSettings();
}

function persistSettings(): void {
  saveSettings({
    ...loadSettings(),
    debugOverlayVisible: isDebugOverlayVisible.value,
    fitToScreen: fitToScreen.value,
    tileAnimations: tileAnimations.value,
    smoothLighting: smoothLighting.value,
  });
}

function retryStartup(): void {
  window.location.reload();
}

function handleGlobalKeydown(event: KeyboardEvent): void {
  if (isMenuOpen.value && clientMenu.value?.handleKeydown(event)) {
    event.stopImmediatePropagation();
    return;
  }
  if (event.ctrlKey && !event.shiftKey && !event.altKey && !event.metaKey && event.key === ',') {
    event.preventDefault();
    event.stopImmediatePropagation();
    if (!event.repeat) isMenuOpen.value = !isMenuOpen.value;
    return;
  }
  if (isMenuOpen.value) {
    event.stopImmediatePropagation();
    if (event.key === 'F5') event.preventDefault();
    if (event.key === 'Escape') { event.preventDefault(); if (!event.repeat) isMenuOpen.value = false; }
    return;
  }
  const active = deepActiveElement();
  const editable = active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement ||
    active instanceof HTMLSelectElement || (active instanceof HTMLElement && active.isContentEditable);
  if (event.key === 'F5' && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
    event.preventDefault();
    event.stopImmediatePropagation();
    if (!event.repeat) window.location.reload();
    return;
  }
  if (keyboardControls.dispatch(event, editable || keyboardControls.hasEditableFocus())) return;
  // Modified F5 combinations must not trigger browser reload over game shortcuts.
  if (event.key === 'F5') event.preventDefault();
}

function deepActiveElement(): Element | null {
  let active = document.activeElement;
  while (active?.shadowRoot?.activeElement) active = active.shadowRoot.activeElement;
  return active;
}

onMounted(() => {
  releases.push(
    keyboardControls.registerBinding({ id: 'client.debug', label: 'Debug Overlay', category: 'General',
      defaultBinding: 'F3', allowInText: true }, () => { setDebugOverlayVisible(!isDebugOverlayVisible.value); }),

  );
  window.addEventListener('keydown', handleGlobalKeydown, true);
});

onBeforeUnmount(() => {
  releases.forEach(release => release());
  keyboardControls.blocked = false;
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
    clientRuntime.setRendererDebugOption('tileAnimations', tileAnimations.value);
    clientRuntime.setRendererDebugOption('smoothLighting', smoothLighting.value);
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
      ref="clientMenu"
      :debug-overlay-visible="isDebugOverlayVisible"
      :fit-to-screen="fitToScreen"
      :tile-animations="tileAnimations"
      :smooth-lighting="smoothLighting"
      @close="isMenuOpen = false"
      @debug-overlay-visible-changed="setDebugOverlayVisible"
      @fit-to-screen-changed="setFitToScreen"
      @tile-animations-changed="setTileAnimations"
      @smooth-lighting-changed="setSmoothLighting"
    />

    <ModDropInstaller />
  </main>
</template>
