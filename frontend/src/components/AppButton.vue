<script setup lang="ts">
defineProps<{
  type?: 'button' | 'submit' | 'reset'
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  disabled?: boolean
  loading?: boolean
  fullWidth?: boolean
}>()
</script>

<template>
  <button
    :type="type ?? 'button'"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded text-sm font-semibold transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
    :class="{
      'w-full': fullWidth,
      'btn-primary': !variant || variant === 'primary',
      'btn-secondary': variant === 'secondary',
      'btn-danger': variant === 'danger',
      'btn-ghost': variant === 'ghost',
    }"
  >
    <svg
      v-if="loading"
      class="w-4 h-4 animate-spin"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
    <slot />
  </button>
</template>

<style scoped>
.btn-primary {
  background-color: #E6A817;
  color: #0D1117;
}
.btn-primary:hover:not(:disabled) {
  background-color: #B8851A;
}

.btn-secondary {
  background-color: transparent;
  color: #E6EDF3;
  border: 1px solid #30363D;
}
.btn-secondary:hover:not(:disabled) {
  border-color: #E6A817;
  color: #E6A817;
}

.btn-danger {
  background-color: transparent;
  color: #F85149;
  border: 1px solid #F85149;
}
.btn-danger:hover:not(:disabled) {
  background-color: #F85149;
  color: #0D1117;
}

.btn-ghost {
  background-color: transparent;
  color: #8B949E;
  border: 1px solid transparent;
}
.btn-ghost:hover:not(:disabled) {
  color: #E6EDF3;
  background-color: #1C2128;
}
</style>
