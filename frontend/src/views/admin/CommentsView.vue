<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { commentsApi } from '@/api/comments'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { AdminCommentResponse, Page } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const searchQuery = ref('')
const currentPage = ref(0)
const pageSize = 20

const pageData = ref<Page<AdminCommentResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Per-row delete in progress
const deleting = ref<Record<number, boolean>>({})

async function fetchComments() {
  loading.value = true
  error.value = null
  try {
    pageData.value = await commentsApi.listAll({
      page: currentPage.value,
      size: pageSize,
      search: searchQuery.value || undefined,
    })
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to load comments.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

// Debounced search
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    fetchComments()
  }, 350)
})

function goToPage(page: number) {
  currentPage.value = page
  fetchComments()
}

async function deleteComment(comment: AdminCommentResponse) {
  if (!window.confirm(`Delete this comment by @${comment.author.username}? This cannot be undone.`)) return

  deleting.value[comment.id] = true
  try {
    await commentsApi.remove(comment.id)
    if (pageData.value) {
      pageData.value.content = pageData.value.content.filter((c) => c.id !== comment.id)
      pageData.value.totalElements -= 1
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to delete comment.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    deleting.value[comment.id] = false
  }
}

onMounted(fetchComments)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// admin.comments</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Comment moderation
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Review and remove comments across all posts.
      </p>
    </div>

    <!-- Search + count bar -->
    <div class="flex flex-col sm:flex-row gap-3 mb-6 items-start sm:items-center justify-between">
      <div class="relative w-full sm:w-80">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none"
          style="color: #8B949E;"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="searchQuery"
          type="search"
          placeholder="Search by content, author, or post…"
          class="w-full pl-9 pr-3 py-2 rounded text-sm transition-all"
          style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif;"
          aria-label="Search comments"
          @focus="($event.target as HTMLInputElement).style.borderColor = '#E6A817'"
          @blur="($event.target as HTMLInputElement).style.borderColor = '#30363D'"
        />
      </div>
      <span class="text-sm shrink-0" style="color: #8B949E;">
        <template v-if="pageData">
          {{ pageData.totalElements }} comment{{ pageData.totalElements === 1 ? '' : 's' }}
        </template>
      </span>
    </div>

    <AlertMessage
      v-if="error"
      type="error"
      :message="error"
      class="mb-6"
    />

    <!-- Loading state -->
    <div
      v-if="loading && !pageData"
      class="flex items-center justify-center py-20"
      style="color: #8B949E;"
    >
      <svg class="w-6 h-6 animate-spin mr-3" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      Loading comments…
    </div>

    <!-- Table -->
    <div
      v-else-if="pageData && pageData.content.length > 0"
      class="rounded-lg overflow-hidden"
      style="border: 1px solid #30363D;"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-sm" style="border-collapse: collapse;">
          <thead>
            <tr style="background-color: #161B22; border-bottom: 1px solid #30363D;">
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Author</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Content</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden md:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Post</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden sm:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Date</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="comment in pageData.content"
              :key="comment.id"
              class="transition-colors"
              style="border-bottom: 1px solid #30363D;"
              :style="{ opacity: deleting[comment.id] ? 0.5 : 1 }"
              @mouseover="($event.currentTarget as HTMLElement).style.backgroundColor = '#1C2128'"
              @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = 'transparent'"
            >
              <!-- Author -->
              <td class="px-4 py-3 align-top">
                <div class="flex items-center gap-2">
                  <div
                    class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0"
                    style="background-color: #1C2128; color: #E6A817; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
                    aria-hidden="true"
                  >
                    {{ comment.author.username.charAt(0).toUpperCase() }}
                  </div>
                  <span class="font-mono text-xs whitespace-nowrap" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
                    @{{ comment.author.username }}
                  </span>
                </div>
              </td>

              <!-- Content excerpt -->
              <td class="px-4 py-3 align-top max-w-xs">
                <p class="text-xs line-clamp-2 leading-relaxed" style="color: #8B949E;">
                  {{ comment.content }}
                </p>
              </td>

              <!-- Post title -->
              <td class="px-4 py-3 align-top hidden md:table-cell">
                <RouterLink
                  :to="`/posts/${comment.postSlug}`"
                  class="text-xs no-underline transition-colors line-clamp-1"
                  style="color: #58A6FF;"
                  @mouseover="($event.currentTarget as HTMLElement).style.color = '#79C0FF'"
                  @mouseleave="($event.currentTarget as HTMLElement).style.color = '#58A6FF'"
                >
                  {{ comment.postTitle }}
                </RouterLink>
              </td>

              <!-- Date -->
              <td class="px-4 py-3 align-top hidden sm:table-cell">
                <time
                  :datetime="comment.createdAt"
                  class="text-xs"
                  style="color: #8B949E;"
                >
                  {{ new Date(comment.createdAt).toLocaleDateString() }}
                </time>
              </td>

              <!-- Actions -->
              <td class="px-4 py-3 align-top text-right">
                <AppButton
                  variant="danger"
                  :disabled="deleting[comment.id]"
                  :loading="deleting[comment.id]"
                  class="text-xs px-3 py-1"
                  @click="deleteComment(comment)"
                >
                  Delete
                </AppButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        v-if="pageData.totalPages > 1"
        class="flex items-center justify-between px-4 py-3"
        style="background-color: #161B22; border-top: 1px solid #30363D;"
      >
        <p class="text-xs" style="color: #8B949E;">
          Page {{ pageData.number + 1 }} of {{ pageData.totalPages }}
        </p>
        <div class="flex gap-2">
          <AppButton
            variant="ghost"
            :disabled="pageData.first || loading"
            class="text-xs px-3 py-1"
            @click="goToPage(currentPage - 1)"
          >
            Previous
          </AppButton>
          <AppButton
            variant="ghost"
            :disabled="pageData.last || loading"
            class="text-xs px-3 py-1"
            @click="goToPage(currentPage + 1)"
          >
            Next
          </AppButton>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div
      v-else-if="pageData && pageData.content.length === 0"
      class="text-center py-16 rounded-lg"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <p class="text-sm" style="color: #8B949E;">
        {{ searchQuery ? 'No comments match your search.' : 'No comments found.' }}
      </p>
      <button
        v-if="searchQuery"
        type="button"
        class="mt-3 text-sm cursor-pointer underline"
        style="color: #E6A817; background: transparent; border: none;"
        @click="searchQuery = ''"
      >
        Clear search
      </button>
    </div>
  </div>
</template>
