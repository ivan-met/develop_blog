<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { postsApi } from '@/api/posts'
import { categoriesApi } from '@/api/categories'
import PostCard from '@/components/PostCard.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { PostSummaryResponse, CategoryResponse, Page } from '@/api/types'

const posts = ref<Page<PostSummaryResponse> | null>(null)
const categories = ref<CategoryResponse[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const searchQuery = ref('')
const selectedCategory = ref<string>('')
const currentPage = ref(0)
const pageSize = 12

// Debounce timer
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchPosts() {
  loading.value = true
  error.value = null
  try {
    posts.value = await postsApi.listPublished({
      category: selectedCategory.value || undefined,
      search: searchQuery.value || undefined,
      page: currentPage.value,
      size: pageSize,
    })
  } catch {
    error.value = 'Failed to load posts. Please try again.'
  } finally {
    loading.value = false
  }
}

watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    fetchPosts()
  }, 350)
})

watch(selectedCategory, () => {
  currentPage.value = 0
  fetchPosts()
})

function goToPage(page: number) {
  currentPage.value = page
  fetchPosts()
}

onMounted(async () => {
  // Fetch categories and first page of posts in parallel
  const [, cats] = await Promise.allSettled([fetchPosts(), categoriesApi.list()])
  if (cats.status === 'fulfilled') {
    categories.value = cats.value
  }
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// posts.feed</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Browse posts
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Technical articles from developers, organized by technology.
      </p>
    </div>

    <!-- Search + filters -->
    <div class="mb-6 flex flex-col gap-4">
      <!-- Search bar -->
      <div class="relative w-full sm:max-w-md">
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

      <!-- Category filter chips -->
      <div
        v-if="categories.length > 0"
        class="flex flex-wrap gap-2"
        role="group"
        aria-label="Filter by category"
      >
        <button
          type="button"
          class="px-3 py-1 rounded text-xs font-mono transition-all cursor-pointer"
          :style="{
            fontFamily: '\'JetBrains Mono\', monospace',
            backgroundColor: selectedCategory === '' ? 'rgba(230, 168, 23, 0.1)' : 'transparent',
            color: selectedCategory === '' ? '#E6A817' : '#8B949E',
            border: `1px solid ${selectedCategory === '' ? 'rgba(230, 168, 23, 0.4)' : '#30363D'}`,
          }"
          @click="selectedCategory = ''"
        >
          All
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          type="button"
          class="px-3 py-1 rounded text-xs font-mono transition-all cursor-pointer"
          :style="{
            fontFamily: '\'JetBrains Mono\', monospace',
            backgroundColor: selectedCategory === cat.slug ? 'rgba(230, 168, 23, 0.1)' : 'transparent',
            color: selectedCategory === cat.slug ? '#E6A817' : '#8B949E',
            border: `1px solid ${selectedCategory === cat.slug ? 'rgba(230, 168, 23, 0.4)' : '#30363D'}`,
          }"
          @click="selectedCategory = cat.slug"
        >
          {{ cat.name }}
        </button>
      </div>
    </div>

    <AlertMessage v-if="error" type="error" :message="error" class="mb-6" />

    <!-- Loading skeleton -->
    <div
      v-if="loading && !posts"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
      aria-busy="true"
      aria-label="Loading posts"
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
      >// no posts found</p>
      <p class="text-sm" style="color: #8B949E;">
        {{ searchQuery || selectedCategory ? 'Try different search terms or a different category.' : 'No posts have been published yet.' }}
      </p>
      <button
        v-if="searchQuery || selectedCategory"
        type="button"
        class="mt-4 text-sm underline cursor-pointer"
        style="color: #E6A817; background: transparent; border: none;"
        @click="searchQuery = ''; selectedCategory = ''"
      >
        Clear filters
      </button>
    </div>

    <!-- Pagination -->
    <div
      v-if="posts && posts.totalPages > 1"
      class="mt-8 flex items-center justify-between"
    >
      <p class="text-xs" style="color: #8B949E;">
        Page {{ posts.number + 1 }} of {{ posts.totalPages }}
        &mdash; {{ posts.totalElements }} post{{ posts.totalElements === 1 ? '' : 's' }}
      </p>
      <div class="flex gap-2">
        <AppButton
          variant="ghost"
          :disabled="posts.first || loading"
          @click="goToPage(currentPage - 1)"
        >
          &larr; Prev
        </AppButton>
        <AppButton
          variant="ghost"
          :disabled="posts.last || loading"
          @click="goToPage(currentPage + 1)"
        >
          Next &rarr;
        </AppButton>
      </div>
    </div>
  </div>
</template>
