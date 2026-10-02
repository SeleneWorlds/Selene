<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { installMod, type ModInstallProgress } from '@/core/services/ModAssetStore';

const state = ref<'idle' | 'dragging' | 'installing' | 'error'>('idle');
const progress = ref<ModInstallProgress | null>(null);
const errorMessage = ref<string | null>(null);
let dragDepth = 0;

function containsFiles(event: DragEvent): boolean {
  return [...(event.dataTransfer?.types ?? [])].includes('Files');
}

function handleDragEnter(event: DragEvent): void {
  if (!containsFiles(event) || state.value === 'installing') return;
  event.preventDefault();
  dragDepth += 1;
  state.value = 'dragging';
}

function handleDragOver(event: DragEvent): void {
  if (!containsFiles(event) || state.value === 'installing') return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy';
}

function handleDragLeave(event: DragEvent): void {
  if (!containsFiles(event) || state.value === 'installing') return;
  dragDepth = Math.max(0, dragDepth - 1);
  if (dragDepth === 0) state.value = 'idle';
}

async function handleDrop(event: DragEvent): Promise<void> {
  if (!containsFiles(event) || state.value === 'installing') return;
  event.preventDefault();
  dragDepth = 0;
  const zip = [...(event.dataTransfer?.files ?? [])]
    .find((file) => file.name.toLocaleLowerCase().endsWith('.zip'));
  if (!zip) {
    state.value = 'error';
    errorMessage.value = 'Drop a .zip file to install a mod.';
    return;
  }
  state.value = 'installing';
  errorMessage.value = null;
  progress.value = { label: `Installing ${zip.name}`, progress: 0 };
  try {
    await installMod(zip, (update) => { progress.value = update; });
    window.location.reload();
  } catch (error) {
    state.value = 'error';
    progress.value = null;
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

function closeError(): void {
  state.value = 'idle';
  errorMessage.value = null;
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && state.value === 'error') closeError();
}

onMounted(() => {
  window.addEventListener('dragenter', handleDragEnter);
  window.addEventListener('dragover', handleDragOver);
  window.addEventListener('dragleave', handleDragLeave);
  window.addEventListener('drop', handleDrop);
  window.addEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('dragenter', handleDragEnter);
  window.removeEventListener('dragover', handleDragOver);
  window.removeEventListener('dragleave', handleDragLeave);
  window.removeEventListener('drop', handleDrop);
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div v-if="state !== 'idle'" class="mod-drop-overlay">
    <section class="mod-drop-overlay__panel" role="status" aria-live="polite">
      <template v-if="state === 'dragging'">
        <strong>Drop archive to install mod</strong>
      </template>
      <template v-else-if="state === 'installing' && progress">
        <strong>{{ progress.label }}</strong>
        <progress :value="progress.progress" max="1">{{ Math.round(progress.progress * 100) }}%</progress>
        <span>{{ Math.round(progress.progress * 100) }}%</span>
      </template>
      <template v-else>
        <strong>Could not install mod</strong>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="closeError">Close</button>
      </template>
    </section>
  </div>
</template>
