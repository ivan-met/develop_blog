<script setup lang="ts">
defineProps<{
  id: string
  type?: string
  placeholder?: string
  modelValue: string
  hasError?: boolean
  disabled?: boolean
  autocomplete?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <input
    :id="id"
    :type="type ?? 'text'"
    :value="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :autocomplete="autocomplete"
    :aria-invalid="hasError ? 'true' : undefined"
    class="w-full px-3 py-2.5 rounded text-sm transition-all"
    :style="{
      backgroundColor: '#161B22',
      color: '#E6EDF3',
      border: `1px solid ${hasError ? '#F85149' : '#30363D'}`,
      outline: 'none',
      fontFamily: 'Inter, sans-serif',
    }"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    @focus="($event.target as HTMLInputElement).style.borderColor = hasError ? '#F85149' : '#E6A817'"
    @blur="($event.target as HTMLInputElement).style.borderColor = hasError ? '#F85149' : '#30363D'"
  />
</template>
