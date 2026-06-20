<script setup lang="ts">
import type { PostStatus } from '@/api/types'

defineProps<{
  status: PostStatus
}>()
</script>

<template>
  <span
    class="inline-flex items-center px-2 py-0.5 rounded text-xs font-mono font-medium"
    :style="{
      fontFamily: '\'JetBrains Mono\', monospace',
      backgroundColor:
        status === 'PUBLISHED'
          ? 'rgba(63, 185, 80, 0.15)'
          : 'rgba(139, 148, 158, 0.15)',
      color: status === 'PUBLISHED' ? '#3FB950' : '#8B949E',
      border: `1px solid ${status === 'PUBLISHED' ? 'rgba(63, 185, 80, 0.3)' : 'rgba(139, 148, 158, 0.3)'}`,
    }"
  >
    {{ status === 'PUBLISHED' ? 'published' : 'draft' }}
  </span>
</template>
