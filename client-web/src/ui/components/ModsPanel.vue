<script setup lang="ts">
import { ref } from 'vue';
import {
  getInstalledMods,
  installMod,
  moveMod,
  removeMod,
  type ModInfo,
  type ModInstallProgress,
} from '@/core/services/ModAssetStore';

const installedMods = ref<ModInfo[]>(getInstalledMods());
const isUpdating = ref(false);
const errorMessage = ref<string | null>(null);
const installProgress = ref<ModInstallProgress | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

async function uploadMod(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  isUpdating.value = true;
  errorMessage.value = null;
  installProgress.value = { label: 'Starting installation', progress: 0 };
  try {
    await installMod(file, (progress) => { installProgress.value = progress; });
    window.location.reload();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
    installProgress.value = null;
    isUpdating.value = false;
  }
}

async function uninstallMod(id: string): Promise<void> {
  await updateMods(() => removeMod(id));
}

async function reorderMod(id: string, direction: -1 | 1): Promise<void> {
  await updateMods(async () => { installedMods.value = await moveMod(id, direction); });
}

async function updateMods(operation: () => Promise<void>): Promise<void> {
  isUpdating.value = true;
  errorMessage.value = null;
  try {
    await operation();
    window.location.reload();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
    isUpdating.value = false;
  }
}
</script>

<template>
  <div class="settings-modal__body mods-panel">
    <div v-if="installedMods.length" class="mods-panel__list">
      <div v-for="(mod, index) in installedMods" :key="mod.id" class="mods-panel__installed">
        <span>
          <strong>{{ mod.name }}</strong>
          <small>{{ mod.fileCount }} file{{ mod.fileCount === 1 ? '' : 's' }} · {{ index === 0 ? 'Highest priority' : `Priority ${index + 1}` }}</small>
        </span>
        <div class="mods-panel__actions">
          <button type="button" aria-label="Move bundle up" :disabled="isUpdating || index === 0" @click="reorderMod(mod.id, -1)">↑</button>
          <button type="button" aria-label="Move bundle down" :disabled="isUpdating || index === installedMods.length - 1" @click="reorderMod(mod.id, 1)">↓</button>
          <button type="button" :disabled="isUpdating" @click="uninstallMod(mod.id)">Remove</button>
        </div>
      </div>
    </div>
    <p v-else class="mods-panel__empty">No mods are installed.</p>

    <p v-if="errorMessage" class="mods-panel__error" role="alert">{{ errorMessage }}</p>
    <div v-if="installProgress" class="mods-panel__progress" role="status" aria-live="polite">
      <div class="mods-panel__progress-label">
        <span>{{ installProgress.label }}</span>
        <span>{{ Math.round(installProgress.progress * 100) }}%</span>
      </div>
      <progress :value="installProgress.progress" max="1">{{ Math.round(installProgress.progress * 100) }}%</progress>
    </div>
    <input ref="fileInput" class="visually-hidden" type="file" accept=".zip,application/zip" @change="uploadMod">
    <button class="mods-panel__upload" type="button" :disabled="isUpdating" @click="fileInput?.click()">
      {{ isUpdating ? 'Installing…' : 'Install Bundle' }}
    </button>
  </div>
</template>
