<script setup lang="ts">
import { useId } from 'vue';

defineProps<{
  title: string;
  showBack?: boolean;
}>();

const titleId = useId();
const emit = defineEmits<{
  back: [];
  close: [];
}>();
</script>

<template>
  <div class="settings-modal" role="presentation" @mousedown.self="emit('close')">
    <section
      class="settings-modal__dialog"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="titleId"
    >
      <header class="settings-modal__header">
        <button
          v-if="showBack"
          class="settings-modal__back"
          type="button"
          aria-label="Back to menu"
          @click="emit('back')"
        >‹</button>
        <h1 :id="titleId">{{ title }}</h1>
        <button
          class="settings-modal__close"
          type="button"
          :aria-label="`Close ${title.toLowerCase()}`"
          @click="emit('close')"
        >×</button>
      </header>
      <slot />
    </section>
  </div>
</template>
