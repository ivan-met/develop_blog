<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { commentsApi } from '@/api/comments'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { CommentResponse, Page } from '@/api/types'

const props = defineProps<{
  slug: string
  /** When set, a newly created comment is prepended without a re-fetch */
  newComment?: CommentResponse | null
}>()

const pageSize = 10
const currentPage = ref(0)
const comments = ref<Page<CommentResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function fetchComments(page = 0) {
  loading.value = true
  error.value = null
  try {
    comments.value = await commentsApi.list(props.slug, {
      page,
      size: pageSize,
    })
    currentPage.value = page
  } catch {
    error.value = 'Failed to load comments.'
  } finally {
    loading.value = false
  }
}

async function deleteComment(id: number) {
  try {
    await commentsApi.remove(id)
    // Refresh current page after deletion
    await fetchComments(currentPage.value)
  } catch {
    error.value = 'Failed to delete comment.'
  }
}

function formatRelativeDate(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d ago`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months}mo ago`
  return `${Math.floor(months / 12)}y ago`
}

function initial(name: string): string {
  return name.charAt(0).toUpperCase()
}

// Watch for a newly posted comment and prepend it
function prependComment(comment: CommentResponse) {
  if (!comments.value) return
  comments.value = {
    ...comments.value,
    content: [comment, ...comments.value.content],
    totalElements: comments.value.totalElements + 1,
  }
}

defineExpose({ prependComment, fetchComments })

onMounted(() => fetchComments(0))
</script>

<template>
  <div>
    <AlertMessage v-if="error" type="error" :message="error" class="mb-4" />

    <!-- Loading -->
    <div v-if="loading && !comments" class="flex items-center gap-2 py-4" style="color: #8B949E;">
      <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      <span class="text-sm font-mono" style="font-family: 'JetBrains Mono', monospace;">Loading comments…</span>
    </div>

    <!-- Empty state -->
    <div
      v-else-if="comments && comments.content.length === 0"
      class="py-6 text-center"
    >
      <p class="text-sm font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
        // no comments yet
      </p>
      <p class="text-xs mt-1" style="color: #8B949E;">Be the first to share your thoughts.</p>
    </div>

    <!-- Comment list -->
    <ul v-else-if="comments" class="divide-y" style="border-color: #30363D;" role="list">
      <li
        v-for="comment in comments.content"
        :key="comment.id"
        class="py-4 first:pt-0"
      >
        <div class="flex gap-3">
          <!-- Avatar -->
          <div
            class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0 mt-0.5"
            style="
              background-color: #1C2128;
              color: #E6A817;
              border: 1px solid #30363D;
              font-family: 'JetBrains Mono', monospace;
            "
            aria-hidden="true"
          >
            {{ initial(comment.author.displayName ?? comment.author.username) }}
          </div>

          <div class="flex-1 min-w-0">
            <!-- Header: author + time + delete -->
            <div class="flex items-center justify-between gap-2 mb-1">
              <div class="flex items-center gap-2 min-w-0">
                <span
                  class="text-xs font-mono font-medium truncate"
                  style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
                >
                  {{ comment.author.displayName ?? comment.author.username }}
                </span>
                <span
                  class="text-xs flex-shrink-0"
                  style="color: #8B949E;"
                  :title="new Date(comment.createdAt).toLocaleString()"
                >
                  {{ formatRelativeDate(comment.createdAt) }}
                </span>
              </div>

              <!-- Delete button: only when canDelete -->
              <button
                v-if="comment.canDelete"
                type="button"
                class="delete-comment-btn flex-shrink-0 p-1 rounded transition-colors cursor-pointer"
                style="background: transparent; border: none; color: #8B949E;"
                :aria-label="`Delete comment by ${comment.author.displayName ?? comment.author.username}`"
                @click="deleteComment(comment.id)"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>

            <!-- Content -->
            <p class="text-sm" style="color: #E6EDF3; line-height: 1.6; word-break: break-word;">
              {{ comment.content }}
            </p>
          </div>
        </div>
      </li>
    </ul>

    <!-- Pagination -->
    <div
      v-if="comments && comments.totalPages > 1"
      class="mt-4 flex items-center justify-between"
    >
      <p class="text-xs" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
        Page {{ comments.number + 1 }} of {{ comments.totalPages }}
      </p>
      <div class="flex gap-2">
        <AppButton
          variant="ghost"
          :disabled="comments.first || loading"
          @click="fetchComments(currentPage - 1)"
        >
          &larr; Prev
        </AppButton>
        <AppButton
          variant="ghost"
          :disabled="comments.last || loading"
          @click="fetchComments(currentPage + 1)"
        >
          Next &rarr;
        </AppButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.delete-comment-btn:hover {
  color: #F85149 !important;
  background-color: rgba(248, 81, 73, 0.08) !important;
}

.divide-y > li + li {
  border-top: 1px solid #30363D;
}
</style>
