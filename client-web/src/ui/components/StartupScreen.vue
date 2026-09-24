<script setup lang="ts">
defineProps<{
  status: 'booting' | 'failed';
  label: string;
  progress: number;
  errorMessage: string | null;
  canRetry: boolean;
}>();

const emit = defineEmits<{
  retry: [];
}>();
</script>

<template>
  <section class="startup-screen" :data-status="status" aria-live="polite">
    <div class="startup-screen__panel">
      <div class="startup-screen__mark" aria-hidden="true">S</div>

      <template v-if="status === 'booting'">
        <h1>Connecting</h1>
        <p>{{ label }}</p>
        <div
          class="startup-screen__progress"
          role="progressbar"
          aria-label="Connection progress"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-valuenow="Math.round(progress * 100)"
        >
          <span :style="{ width: `${progress * 100}%` }" />
        </div>
      </template>

      <template v-else>
        <h1>Unable to connect</h1>
        <p class="startup-screen__error">{{ errorMessage || 'An unexpected error occurred.' }}</p>
        <button v-if="canRetry" type="button" @click="emit('retry')">Try again</button>
      </template>
    </div>
  </section>
</template>
