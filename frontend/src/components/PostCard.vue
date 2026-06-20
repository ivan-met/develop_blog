<script setup lang="ts">
import type { PostSummaryResponse } from '@/api/types'
import StatusBadge from '@/components/StatusBadge.vue'

defineProps<{
  post: PostSummaryResponse
  showStatus?: boolean
}>()

const emit = defineEmits<{
  tagClick: [tag: string]
}>()

function formatDate(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function formatCount(count: number): string {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1).replace(/\.0$/, '')}k`
  }
  return String(count)
}

// Keep the old name as an alias for backward compatibility
function formatViewCount(count: number): string {
  return formatCount(count)
}

function handleTagClick(tag: string) {
  emit('tagClick', tag)
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
      <!-- Category chip + status -->
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
        class="text-sm mb-3 line-clamp-3"
        style="color: #8B949E; line-height: 1.6;"
      >
        {{ post.excerpt }}
      </p>

      <!-- Tags -->
      <div
        v-if="post.tags && post.tags.length > 0"
        class="flex flex-wrap gap-1.5 mb-3"
      >
        <button
          v-for="tag in post.tags"
          :key="tag"
          type="button"
          class="text-xs px-2 py-0.5 rounded transition-all cursor-pointer"
          style="
            font-family: Inter, sans-serif;
            background-color: rgba(56, 139, 253, 0.08);
            color: #79C0FF;
            border: 1px solid rgba(56, 139, 253, 0.25);
            outline: none;
          "
          :aria-label="`Search for tag: ${tag}`"
          @click.prevent.stop="handleTagClick(tag)"
          @focus="($event.target as HTMLElement).style.outlineColor = 'rgba(230, 168, 23, 0.5)'; ($event.target as HTMLElement).style.outlineWidth = '2px'; ($event.target as HTMLElement).style.outlineStyle = 'solid'; ($event.target as HTMLElement).style.outlineOffset = '2px'"
          @blur="($event.target as HTMLElement).style.outline = 'none'"
          @mouseover="($event.target as HTMLElement).style.borderColor = 'rgba(56, 139, 253, 0.5)'"
          @mouseleave="($event.target as HTMLElement).style.borderColor = 'rgba(56, 139, 253, 0.25)'"
        >
          {{ tag }}
        </button>
      </div>

      <!-- Footer: author + date + viewCount -->
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
        <div class="flex items-center gap-3 flex-shrink-0">
          <!-- Like count (read-only) -->
          <span
            class="text-xs flex items-center gap-1"
            style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
            :aria-label="`${post.likeCount} likes`"
          >
            <!-- Heart icon -->
            <svg
              class="w-3.5 h-3.5 flex-shrink-0"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
            </svg>
            {{ formatCount(post.likeCount) }}
          </span>
          <!-- View count -->
          <span
            class="text-xs flex items-center gap-1"
            style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
            :aria-label="`${post.viewCount} views`"
          >
            <!-- Eye icon -->
            <svg
              class="w-3.5 h-3.5 flex-shrink-0"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            {{ formatViewCount(post.viewCount) }}
          </span>
          <span class="text-xs" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
            {{ formatDate(post.publishedAt ?? post.createdAt) }}
          </span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>
