<script setup lang="ts">
defineProps<{
  type: 'error' | 'success' | 'info'
  message: string
}>()
</script>

<template>
  <div
    class="rounded px-4 py-3 text-sm"
    :style="{
      backgroundColor: type === 'error' ? 'rgba(248, 81, 73, 0.1)' : type === 'success' ? 'rgba(63, 185, 80, 0.1)' : 'rgba(230, 168, 23, 0.1)',
      border: `1px solid ${type === 'error' ? '#F85149' : type === 'success' ? '#3FB950' : '#E6A817'}`,
      color: type === 'error' ? '#F85149' : type === 'success' ? '#3FB950' : '#E6A817',
    }"
    role="alert"
    aria-live="polite"
  >
    {{ message }}
  </div>
</template>
