<script setup lang="ts">
import type { PostSummaryResponse } from '@/api/types'
import StatusBadge from '@/components/StatusBadge.vue'

defineProps<{
  post: PostSummaryResponse
  showStatus?: boolean
}>()

function formatDate(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<template>
  <RouterLink
    :to="`/posts/${post.slug}`"
    class="post-card block no-underline rounded"
    style="background-color: #161B22; border: 1px solid #30363D; transition: border-color 0.15s, background-color 0.15s;"
    @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#8B949E'"
    @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
  >
    <div class="p-5">
      <!-- Category chip -->
      <div class="mb-3 flex items-center justify-between gap-2">
        <span
          v-if="post.category"
          class="text-xs font-mono px-2 py-0.5 rounded"
          style="
            font-family: 'JetBrains Mono', monospace;
            background-color: rgba(230, 168, 23, 0.1);
            color: #E6A817;
            border: 1px solid rgba(230, 168, 23, 0.25);
          "
        >
          {{ post.category.name }}
        </span>
        <span v-else class="text-xs font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">uncategorized</span>
        <StatusBadge v-if="showStatus" :status="post.status" />
      </div>

      <!-- Title -->
      <h2
        class="text-base font-mono font-semibold mb-2 line-clamp-2"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace; line-height: 1.4;"
      >
        {{ post.title }}
      </h2>

      <!-- Excerpt -->
      <p
        v-if="post.excerpt"
        class="text-sm mb-4 line-clamp-3"
        style="color: #8B949E; line-height: 1.6;"
      >
        {{ post.excerpt }}
      </p>

      <!-- Footer: author + date -->
      <div class="flex items-center justify-between gap-2 pt-3" style="border-top: 1px solid #30363D;">
        <div class="flex items-center gap-2 min-w-0">
          <div
            class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0"
            style="background-color: #1C2128; color: #E6A817; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
          >
            {{ (post.author.displayName ?? post.author.username).charAt(0).toUpperCase() }}
          </div>
          <span class="text-xs truncate" style="color: #8B949E;">
            {{ post.author.displayName ?? post.author.username }}
          </span>
        </div>
        <span class="text-xs flex-shrink-0" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
          {{ formatDate(post.publishedAt ?? post.createdAt) }}
        </span>
      </div>
    </div>
  </RouterLink>
</template>
