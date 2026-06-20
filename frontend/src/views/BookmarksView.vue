<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { engagementApi } from '@/api/engagement'
import PostCard from '@/components/PostCard.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { PostSummaryResponse, Page } from '@/api/types'

const posts = ref<Page<PostSummaryResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(0)
const pageSize = 12

async function fetchBookmarks(page = 0) {
  loading.value = true
  error.value = null
  try {
    posts.value = await engagementApi.listMyBookmarks({ page, size: pageSize })
    currentPage.value = page
  } catch {
    error.value = 'Failed to load bookmarks. Please try again.'
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchBookmarks(0))
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// bookmarks</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Saved posts
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Posts you've bookmarked for later reading.
      </p>
    </div>

    <AlertMessage v-if="error" type="error" :message="error" class="mb-6" />

    <!-- Loading skeleton -->
    <div
      v-if="loading && !posts"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
      aria-busy="true"
      aria-label="Loading bookmarks"
    >
      <div
        v-for="n in 6"
        :key="n"
        class="rounded h-52 animate-pulse"
        style="background-color: #161B22; border: 1px solid #30363D;"
      />
    </div>

    <!-- Posts grid -->
    <div
      v-else-if="posts && posts.content.length > 0"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
    >
      <PostCard
        v-for="post in posts.content"
        :key="post.id"
        :post="post"
      />
    </div>

    <!-- Empty state -->
    <div
      v-else-if="posts && posts.content.length === 0"
      class="text-center py-20 rounded"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <p
        class="font-mono text-sm mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
      >// no bookmarks yet</p>
      <p class="text-sm mb-6" style="color: #8B949E;">
        Posts you bookmark will appear here.
      </p>
      <RouterLink
        to="/"
        class="text-sm underline"
        style="color: #E6A817;"
      >
        Browse posts
      </RouterLink>
    </div>

    <!-- Pagination -->
    <div
      v-if="posts && posts.totalPages > 1"
      class="mt-8 flex items-center justify-between"
    >
      <p class="text-xs" style="color: #8B949E;">
        Page {{ posts.number + 1 }} of {{ posts.totalPages }}
        &mdash; {{ posts.totalElements }} saved post{{ posts.totalElements === 1 ? '' : 's' }}
      </p>
      <div class="flex gap-2">
        <AppButton
          variant="ghost"
          :disabled="posts.first || loading"
          @click="fetchBookmarks(currentPage - 1)"
        >
          &larr; Prev
        </AppButton>
        <AppButton
          variant="ghost"
          :disabled="posts.last || loading"
          @click="fetchBookmarks(currentPage + 1)"
        >
          Next &rarr;
        </AppButton>
      </div>
    </div>
  </div>
</template>
