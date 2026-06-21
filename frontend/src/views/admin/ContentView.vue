<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { postsApi } from '@/api/posts'
import { categoriesApi } from '@/api/categories'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { PostSummaryResponse, CategoryResponse, Page, PostStatus } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const searchQuery = ref('')
const statusFilter = ref<PostStatus | ''>('')
const categoryFilter = ref('')
const currentPage = ref(0)
const pageSize = 20

const pageData = ref<Page<PostSummaryResponse> | null>(null)
const categories = ref<CategoryResponse[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// Per-row action in progress
const acting = ref<Record<number, boolean>>({})

async function fetchPosts() {
  loading.value = true
  error.value = null
  try {
    pageData.value = await postsApi.listAdmin({
      page: currentPage.value,
      size: pageSize,
      status: statusFilter.value || undefined,
      search: searchQuery.value || undefined,
      categorySlug: categoryFilter.value || undefined,
    })
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to load posts.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    categories.value = await categoriesApi.list()
  } catch {
    // non-critical — filter still works without categories list
  }
}

// Debounced search
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    fetchPosts()
  }, 350)
})

watch([statusFilter, categoryFilter], () => {
  currentPage.value = 0
  fetchPosts()
})

function goToPage(page: number) {
  currentPage.value = page
  fetchPosts()
}

async function toggleStatus(post: PostSummaryResponse) {
  const nextStatus: PostStatus = post.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  acting.value[post.id] = true
  try {
    const updated = await postsApi.changeStatus(post.id, { status: nextStatus })
    if (pageData.value) {
      const idx = pageData.value.content.findIndex((p) => p.id === post.id)
      if (idx !== -1) pageData.value.content[idx] = updated
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? `Failed to update post status.`
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    acting.value[post.id] = false
  }
}

async function deletePost(post: PostSummaryResponse) {
  if (!window.confirm(`Delete "${post.title}"? This cannot be undone.`)) return

  acting.value[post.id] = true
  try {
    await postsApi.remove(post.id)
    if (pageData.value) {
      pageData.value.content = pageData.value.content.filter((p) => p.id !== post.id)
      pageData.value.totalElements -= 1
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to delete post.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    acting.value[post.id] = false
  }
}

onMounted(() => {
  fetchPosts()
  fetchCategories()
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// admin.content</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Content management
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        All posts from all authors — publish, unpublish, or remove content.
      </p>
    </div>

    <!-- Filters bar -->
    <div class="flex flex-col sm:flex-row gap-3 mb-6 flex-wrap">
      <!-- Search -->
      <div class="relative flex-1 min-w-48">
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
          placeholder="Search posts…"
          class="w-full pl-9 pr-3 py-2 rounded text-sm transition-all"
          style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif;"
          aria-label="Search posts"
          @focus="($event.target as HTMLInputElement).style.borderColor = '#E6A817'"
          @blur="($event.target as HTMLInputElement).style.borderColor = '#30363D'"
        />
      </div>

      <!-- Status filter -->
      <select
        v-model="statusFilter"
        class="py-2 px-3 rounded text-sm cursor-pointer"
        style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif;"
        aria-label="Filter by status"
      >
        <option value="">All statuses</option>
        <option value="PUBLISHED">Published</option>
        <option value="DRAFT">Draft</option>
      </select>

      <!-- Category filter -->
      <select
        v-model="categoryFilter"
        class="py-2 px-3 rounded text-sm cursor-pointer"
        style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif;"
        aria-label="Filter by category"
      >
        <option value="">All categories</option>
        <option
          v-for="cat in categories"
          :key="cat.slug"
          :value="cat.slug"
        >
          {{ cat.name }}
        </option>
      </select>

      <span class="text-sm self-center shrink-0" style="color: #8B949E;">
        <template v-if="pageData">
          {{ pageData.totalElements }} post{{ pageData.totalElements === 1 ? '' : 's' }}
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
      Loading posts…
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
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Title</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden sm:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Author</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden md:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Category</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Status</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider hidden lg:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Views</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider hidden lg:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Likes</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden xl:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Created</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="post in pageData.content"
              :key="post.id"
              class="transition-colors"
              style="border-bottom: 1px solid #30363D;"
              :style="{ opacity: acting[post.id] ? 0.5 : 1 }"
              @mouseover="($event.currentTarget as HTMLElement).style.backgroundColor = '#1C2128'"
              @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = 'transparent'"
            >
              <!-- Title -->
              <td class="px-4 py-3 align-middle max-w-xs">
                <RouterLink
                  :to="`/posts/${post.slug}`"
                  class="text-sm font-medium no-underline transition-colors line-clamp-1 block"
                  style="color: #E6EDF3;"
                  @mouseover="($event.currentTarget as HTMLElement).style.color = '#E6A817'"
                  @mouseleave="($event.currentTarget as HTMLElement).style.color = '#E6EDF3'"
                >
                  {{ post.title }}
                </RouterLink>
                <div v-if="post.tags.length > 0" class="flex flex-wrap gap-1 mt-1">
                  <span
                    v-for="tag in post.tags.slice(0, 3)"
                    :key="tag"
                    class="text-xs px-1.5 py-0.5 rounded font-mono"
                    style="background-color: #1C2128; color: #8B949E; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
                  >{{ tag }}</span>
                </div>
              </td>

              <!-- Author -->
              <td class="px-4 py-3 align-middle hidden sm:table-cell">
                <span class="font-mono text-xs" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
                  @{{ post.author.username }}
                </span>
              </td>

              <!-- Category -->
              <td class="px-4 py-3 align-middle hidden md:table-cell">
                <span v-if="post.category" class="text-xs" style="color: #8B949E;">
                  {{ post.category.name }}
                </span>
                <span v-else class="text-xs" style="color: #30363D;">&mdash;</span>
              </td>

              <!-- Status -->
              <td class="px-4 py-3 align-middle">
                <StatusBadge :status="post.status" />
              </td>

              <!-- Views -->
              <td class="px-4 py-3 align-middle text-right hidden lg:table-cell">
                <span class="text-xs font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
                  {{ post.viewCount.toLocaleString() }}
                </span>
              </td>

              <!-- Likes -->
              <td class="px-4 py-3 align-middle text-right hidden lg:table-cell">
                <span class="text-xs font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
                  {{ post.likeCount }}
                </span>
              </td>

              <!-- Created -->
              <td class="px-4 py-3 align-middle hidden xl:table-cell">
                <time :datetime="post.createdAt" class="text-xs" style="color: #8B949E;">
                  {{ new Date(post.createdAt).toLocaleDateString() }}
                </time>
              </td>

              <!-- Actions -->
              <td class="px-4 py-3 align-middle text-right">
                <div class="flex items-center justify-end gap-2">
                  <AppButton
                    :variant="post.status === 'PUBLISHED' ? 'secondary' : 'ghost'"
                    :disabled="acting[post.id]"
                    :loading="acting[post.id]"
                    class="text-xs px-3 py-1 whitespace-nowrap"
                    @click="toggleStatus(post)"
                  >
                    {{ post.status === 'PUBLISHED' ? 'Unpublish' : 'Publish' }}
                  </AppButton>
                  <AppButton
                    variant="danger"
                    :disabled="acting[post.id]"
                    :loading="acting[post.id]"
                    class="text-xs px-3 py-1"
                    @click="deletePost(post)"
                  >
                    Delete
                  </AppButton>
                </div>
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
        {{ searchQuery || statusFilter || categoryFilter ? 'No posts match your filters.' : 'No posts found.' }}
      </p>
      <button
        v-if="searchQuery || statusFilter || categoryFilter"
        type="button"
        class="mt-3 text-sm cursor-pointer underline"
        style="color: #E6A817; background: transparent; border: none;"
        @click="searchQuery = ''; statusFilter = ''; categoryFilter = ''"
      >
        Clear filters
      </button>
    </div>
  </div>
</template>
