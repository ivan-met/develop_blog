<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { CSSProperties } from 'vue'
import { categoriesApi } from '@/api/categories'
import type { CategoryResponse } from '@/api/types'

const props = defineProps<{
  modelValue: number | null
  hasError?: boolean
  disabled?: boolean
}>()

const chevron =
  "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%238B949E' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E\")"

const selectStyle = computed<CSSProperties>(() => ({
  backgroundColor: '#161B22',
  color: props.modelValue ? '#E6EDF3' : '#8B949E',
  border: `1px solid ${props.hasError ? '#F85149' : '#30363D'}`,
  outline: 'none',
  fontFamily: 'Inter, sans-serif',
  appearance: 'none',
  backgroundImage: chevron,
  backgroundRepeat: 'no-repeat',
  backgroundPosition: 'right 0.75rem center',
  paddingRight: '2.5rem',
}))

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const categories = ref<CategoryResponse[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    categories.value = await categoriesApi.list()
  } catch {
    // non-critical — select will show empty
  } finally {
    loading.value = false
  }
})

function onChange(event: Event) {
  const val = (event.target as HTMLSelectElement).value
  emit('update:modelValue', val ? Number(val) : null)
}
</script>

<template>
  <select
    :value="props.modelValue ?? ''"
    :disabled="disabled || loading"
    :aria-invalid="hasError ? 'true' : undefined"
    class="w-full px-3 py-2.5 rounded text-sm transition-all cursor-pointer"
    :style="selectStyle"
    @change="onChange"
    @focus="($event.target as HTMLSelectElement).style.borderColor = hasError ? '#F85149' : '#E6A817'"
    @blur="($event.target as HTMLSelectElement).style.borderColor = hasError ? '#F85149' : '#30363D'"
  >
    <option value="">
      {{ loading ? 'Loading…' : 'Select a category (optional for draft)' }}
    </option>
    <option
      v-for="cat in categories"
      :key="cat.id"
      :value="cat.id"
    >
      {{ cat.name }}
    </option>
  </select>
</template>
