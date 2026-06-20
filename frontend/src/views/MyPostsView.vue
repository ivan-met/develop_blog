<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { postsApi } from '@/api/posts'
import AppButton from '@/components/AppButton.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { PostSummaryResponse, Page, PostStatus } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const router = useRouter()

type StatusFilter = 'ALL' | PostStatus
const statusFilter = ref<StatusFilter>('ALL')

const posts = ref<Page<PostSummaryResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(0)
const pageSize = 20

// Per-row action state
const actioning = ref<Record<number, boolean>>({})

async function fetchPosts() {
  loading.value = true
  error.value = null
  try {
    posts.value = await postsApi.listMine({
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      page: currentPage.value,
      size: pageSize,
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

watch(statusFilter, () => {
  currentPage.value = 0
  fetchPosts()
})

function goToPage(page: number) {
  currentPage.value = page
  fetchPosts()
}

function editPost(post: PostSummaryResponse) {
  router.push(`/posts/${post.id}/edit`)
}

async function togglePublish(post: PostSummaryResponse) {
  actioning.value[post.id] = true
  error.value = null
  try {
    const newStatus: PostStatus = post.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
    const updated = await postsApi.changeStatus(post.id, { status: newStatus })
    if (posts.value) {
      const idx = posts.value.content.findIndex((p) => p.id === post.id)
      if (idx !== -1) {
        posts.value.content[idx] = { ...posts.value.content[idx], status: updated.status }
      }
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to update post status.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    actioning.value[post.id] = false
  }
}

async function deletePost(post: PostSummaryResponse) {
  if (!confirm(`Delete "${post.title}"? This cannot be undone.`)) return

  actioning.value[post.id] = true
  error.value = null
  try {
    await postsApi.remove(post.id)
    if (posts.value) {
      posts.value.content = posts.value.content.filter((p) => p.id !== post.id)
      posts.value.totalElements -= 1
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to delete post.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    actioning.value[post.id] = false
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

onMounted(fetchPosts)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
    <!-- Page header -->
    <div class="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <p
          class="text-xs font-mono mb-2"
          style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          aria-hidden="true"
        >// me.posts</p>
        <h1
          class="text-2xl font-mono font-semibold"
          style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
        >
          My posts
        </h1>
        <p class="text-sm mt-1" style="color: #8B949E;">
          Manage your drafts and published articles.
        </p>
      </div>
      <RouterLink
        to="/posts/new"
        class="inline-flex items-center gap-2 px-4 py-2 rounded text-sm font-semibold no-underline transition-colors"
        style="background-color: #E6A817; color: #0D1117;"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        New post
      </RouterLink>
    </div>

    <!-- Status filter tabs -->
    <div
      class="flex gap-0 mb-6"
      role="tablist"
      aria-label="Filter by status"
    >
      <button
        v-for="tab in (['ALL', 'DRAFT', 'PUBLISHED'] as StatusFilter[])"
        :key="tab"
        type="button"
        role="tab"
        :aria-selected="statusFilter === tab"
        class="px-4 py-2 text-xs font-mono font-medium transition-colors cursor-pointer"
        :style="{
          fontFamily: '\'JetBrains Mono\', monospace',
          background: 'transparent',
          border: 'none',
          borderBottom: statusFilter === tab ? '2px solid #E6A817' : '2px solid transparent',
          color: statusFilter === tab ? '#E6A817' : '#8B949E',
          paddingBottom: '0.4rem',
        }"
        @click="statusFilter = tab"
      >
        {{ tab === 'ALL' ? 'All' : tab === 'DRAFT' ? 'Drafts' : 'Published' }}
      </button>
    </div>

    <AlertMessage v-if="error" type="error" :message="error" class="mb-6" />

    <!-- Loading -->
    <div
      v-if="loading && !posts"
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
      v-else-if="posts && posts.content.length > 0"
      class="rounded overflow-hidden"
      style="border: 1px solid #30363D;"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-sm" style="border-collapse: collapse;">
          <thead>
            <tr style="background-color: #161B22; border-bottom: 1px solid #30363D;">
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Title</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden sm:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Category</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Status</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden md:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Date</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="post in posts.content"
              :key="post.id"
              class="transition-colors"
              style="border-bottom: 1px solid #30363D;"
              :style="{ opacity: actioning[post.id] ? 0.6 : 1 }"
              @mouseover="($event.currentTarget as HTMLElement).style.backgroundColor = '#1C2128'"
              @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = 'transparent'"
            >
              <!-- Title -->
              <td class="px-4 py-3">
                <RouterLink
                  v-if="post.status === 'PUBLISHED'"
                  :to="`/posts/${post.slug}`"
                  class="font-medium no-underline hover:underline line-clamp-2"
                  style="color: #E6EDF3;"
                >
                  {{ post.title }}
                </RouterLink>
                <span v-else class="font-medium line-clamp-2" style="color: #E6EDF3;">
                  {{ post.title }}
                </span>
              </td>

              <!-- Category -->
              <td class="px-4 py-3 hidden sm:table-cell">
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
                <span v-else class="text-xs" style="color: #8B949E;">&mdash;</span>
              </td>

              <!-- Status -->
              <td class="px-4 py-3">
                <StatusBadge :status="post.status" />
              </td>

              <!-- Date -->
              <td class="px-4 py-3 text-xs hidden md:table-cell" style="color: #8B949E;">
                {{ formatDate(post.publishedAt ?? post.createdAt) }}
              </td>

              <!-- Actions -->
              <td class="px-4 py-3">
                <div class="flex items-center justify-end gap-2 flex-wrap">
                  <AppButton
                    variant="ghost"
                    :disabled="actioning[post.id]"
                    class="text-xs px-2 py-1"
                    @click="editPost(post)"
                  >
                    Edit
                  </AppButton>
                  <AppButton
                    variant="secondary"
                    :disabled="actioning[post.id]"
                    :loading="actioning[post.id]"
                    class="text-xs px-2 py-1"
                    @click="togglePublish(post)"
                  >
                    {{ post.status === 'PUBLISHED' ? 'Unpublish' : 'Publish' }}
                  </AppButton>
                  <AppButton
                    variant="danger"
                    :disabled="actioning[post.id]"
                    class="text-xs px-2 py-1"
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
        v-if="posts.totalPages > 1"
        class="flex items-center justify-between px-4 py-3"
        style="background-color: #161B22; border-top: 1px solid #30363D;"
      >
        <p class="text-xs" style="color: #8B949E;">
          Page {{ posts.number + 1 }} of {{ posts.totalPages }}
        </p>
        <div class="flex gap-2">
          <AppButton
            variant="ghost"
            :disabled="posts.first || loading"
            class="text-xs px-3 py-1"
            @click="goToPage(currentPage - 1)"
          >
            Previous
          </AppButton>
          <AppButton
            variant="ghost"
            :disabled="posts.last || loading"
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
      v-else-if="posts && posts.content.length === 0"
      class="text-center py-16 rounded"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <p
        class="font-mono text-sm mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
      >
        {{ statusFilter === 'DRAFT' ? '// no drafts' : statusFilter === 'PUBLISHED' ? '// no published posts' : '// no posts yet' }}
      </p>
      <p class="text-sm mb-4" style="color: #8B949E;">
        {{ statusFilter === 'ALL' ? 'Write your first post to get started.' : `You have no ${statusFilter === 'DRAFT' ? 'drafts' : 'published posts'}.` }}
      </p>
      <RouterLink
        to="/posts/new"
        class="inline-flex items-center gap-1 text-sm no-underline underline"
        style="color: #E6A817;"
      >
        Create a post
      </RouterLink>
    </div>
  </div>
</template>
