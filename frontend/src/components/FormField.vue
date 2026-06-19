<script setup lang="ts">
defineProps<{
  id: string
  label: string
  error?: string
  hint?: string
}>()
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label
      :for="id"
      class="text-xs font-mono font-medium tracking-wide uppercase"
      style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
    >
      {{ label }}
    </label>
    <slot />
    <p
      v-if="error"
      :id="`${id}-error`"
      class="text-xs mt-0.5"
      style="color: #F85149;"
      role="alert"
    >
      {{ error }}
    </p>
    <p
      v-else-if="hint"
      class="text-xs mt-0.5"
      style="color: #8B949E;"
    >
      {{ hint }}
    </p>
  </div>
</template>
