<script setup lang="ts">
import type { AuthorSummary } from '@/api/types'

defineProps<{
  author: AuthorSummary
}>()

function initial(author: AuthorSummary): string {
  return (author.displayName ?? author.username).charAt(0).toUpperCase()
}
</script>

<template>
  <RouterLink
    :to="`/authors/${author.username}`"
    class="author-byline inline-flex items-center gap-1.5 no-underline transition-colors"
    :aria-label="`View profile of ${author.displayName ?? author.username}`"
  >
    <span
      class="w-5 h-5 rounded-full inline-flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0"
      style="
        background-color: #1C2128;
        color: #E6A817;
        border: 1px solid #30363D;
        font-family: 'JetBrains Mono', monospace;
      "
      aria-hidden="true"
    >
      {{ initial(author) }}
    </span>
    <span class="text-sm author-byline__name" style="color: #8B949E;">
      {{ author.displayName ?? author.username }}
    </span>
  </RouterLink>
</template>

<style scoped>
.author-byline:hover .author-byline__name {
  color: #E6A817;
}
</style>
