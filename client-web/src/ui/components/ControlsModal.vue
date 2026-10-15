<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import BaseModal from './BaseModal.vue';
import { keyboardControls } from '../controls';
import { formatBinding, type Shortcut } from '../KeyboardControls';

const emit = defineEmits<{ back: []; close: [] }>();
const revision = ref(0);
const recording = ref<string | null>(null);
const status = ref('');
const error = ref(false);
const body = ref<HTMLElement | null>(null);
const unsubscribe = keyboardControls.subscribe(() => { revision.value++; });
const groups = computed(() => {
  void revision.value;
  const groups = new Map<string, Shortcut[]>();
  for (const action of keyboardControls.list()) {
    const category = action.category ?? 'Other';
    const actions = groups.get(category) ?? [];
    actions.push(action);
    groups.set(category, actions);
  }
  return [...groups].map(([category, actions]) => ({ category, actions }));
});

function bindingLabel(action: Shortcut): string {
  void revision.value;
  return formatBinding(keyboardControls.binding(action)) ?? 'Unbound';
}

function update(operation: () => void): void {
  try {
    operation();
    recording.value = null;
    status.value = 'Saved.';
    error.value = false;
  } catch (reason) {
    status.value = reason instanceof Error ? reason.message : String(reason);
    error.value = true;
  }
}

function startRecording(action: Shortcut): void {
  recording.value = action.id;
  status.value = `Press a key combination for ${action.label}. Escape unbinds.`;
  error.value = false;
}

function handleKeydown(event: KeyboardEvent): boolean {
  if (recording.value) {
    event.preventDefault();
    if (event.repeat) return true;
    if (event.key === 'Escape') {
      const id = recording.value;
      update(() => keyboardControls.setBinding(id, null));
    } else if (!['Control', 'Shift', 'Alt', 'Meta'].includes(event.key)) {
      const id = recording.value;
      update(() => keyboardControls.setBinding(id, formatBinding(event)));
    }
    return true;
  }
  if (event.key === 'Tab') {
    const dialog = body.value?.closest('.settings-modal__dialog');
    const buttons = [...(dialog?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [])];
    const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
    const next = (index + (event.shiftKey ? -1 : 1) + buttons.length) % buttons.length;
    buttons[next]?.focus();
    event.preventDefault();
    return true;
  }
  return false;
}
defineExpose({ handleKeydown });
onMounted(() => { void nextTick(() => body.value?.querySelector<HTMLButtonElement>('button')?.focus()); });
onBeforeUnmount(unsubscribe);
</script>

<template>
  <BaseModal title="Controls" show-back @back="emit('back')" @close="emit('close')">
    <div ref="body" class="controls-modal">
      <div class="controls-list">
        <section v-for="group in groups" :key="group.category">
          <h2>{{ group.category }}</h2>
          <div v-for="action in group.actions" :key="action.id" class="control-row">
            <span>{{ action.label }}</span>
            <button type="button" :class="{ recording: recording === action.id }"
              :aria-label="`Rebind ${action.label}`" @click="startRecording(action)">
              {{ recording === action.id ? 'Press a key…' : bindingLabel(action) }}
            </button>
            <button type="button" :aria-label="`Reset ${action.label}`"
              @click="update(() => keyboardControls.reset(action.id))">Reset</button>
          </div>
        </section>
      </div>
      <footer>
        <span role="status" aria-live="polite" :class="{ error }">{{ status }}</span>
      </footer>
    </div>
  </BaseModal>
</template>

<style scoped>
.controls-modal { padding: 0 20px 20px; }
p { color: var(--color-neutral-400); font-size: 13px; margin: 0 0 10px; }
.controls-list { max-height: 55vh; overflow: auto; padding-right: 16px; scrollbar-gutter: stable; }
h2 { font-size: 14px; margin: 18px 0 8px; }
.control-row { display: grid; grid-template-columns: minmax(100px, 1fr) minmax(85px, 120px) auto; gap: 6px; align-items: center; padding: 6px 0; border-top: 1px solid #ffffff12; font-size: 13px; }
button { border: 1px solid #ffffff30; border-radius: 4px; padding: 6px 8px; background: #ffffff08; color: inherit; cursor: pointer; }
button:hover, button:focus-visible { background: #ffffff20; }
button:disabled { opacity: .4; cursor: default; }
.recording { background: #715b27; }
footer { margin-top: 15px; display: flex; gap: 10px; align-items: center; }
footer span { flex: 1; font-size: 13px; }
.error { color: #ffb4a8; }
@media (max-width: 520px) {
  .control-row { grid-template-columns: minmax(0, 1fr) auto; }
  .control-row > span { grid-column: 1 / -1; }
}
</style>
