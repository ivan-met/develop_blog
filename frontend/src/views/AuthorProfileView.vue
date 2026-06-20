<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { authorsApi } from '@/api/authors'
import PostCard from '@/components/PostCard.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { AuthorProfileResponse, PostSummaryResponse, Page } from '@/api/types'
import axios from 'axios'

const route = useRoute()

const profile = ref<AuthorProfileResponse | null>(null)
const posts = ref<Page<PostSummaryResponse> | null>(null)
const loading = ref(true)
const postsLoading = ref(false)
const error = ref<string | null>(null)
const notFound = ref(false)
const currentPage = ref(0)
const pageSize = 12

function formatJoinDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
  })
}

async function fetchPosts(page = 0) {
  if (!profile.value) return
  postsLoading.value = true
  try {
    posts.value = await authorsApi.getPosts(profile.value.username, {
      page,
      size: pageSize,
    })
    currentPage.value = page
  } catch {
    error.value = 'Failed to load posts.'
  } finally {
    postsLoading.value = false
  }
}

onMounted(async () => {
  const username = route.params.username as string
  try {
    profile.value = await authorsApi.getProfile(username)
    await fetchPosts(0)
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      notFound.value = true
    } else {
      error.value = 'Failed to load author profile.'
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-20" style="color: #8B949E;">
      <svg class="w-6 h-6 animate-spin" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
    </div>

    <!-- 404 -->
    <div v-else-if="notFound" class="text-center py-20">
      <p
        class="font-mono text-sm mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
      >// 404.not_found</p>
      <h1 class="text-xl font-mono font-semibold mb-2" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
        Author not found
      </h1>
      <p class="text-sm mb-6" style="color: #8B949E;">
        This author profile does not exist.
      </p>
      <RouterLink to="/" class="text-sm underline" style="color: #E6A817;">
        Back to posts
      </RouterLink>
    </div>

    <!-- Error -->
    <AlertMessage v-else-if="error && !profile" type="error" :message="error" />

    <!-- Profile -->
    <template v-else-if="profile">
      <!-- Sigil -->
      <p
        class="text-xs font-mono mb-6"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// author.profile</p>

      <!-- Profile header card -->
      <div
        class="rounded p-6 mb-10 flex flex-col sm:flex-row gap-6 items-start"
        style="background-color: #161B22; border: 1px solid #30363D;"
      >
        <!-- Avatar: terminal cursor block style -->
        <div
          class="author-avatar flex-shrink-0 w-16 h-16 rounded flex items-center justify-center font-mono font-bold text-3xl"
          style="
            background-color: #E6A817;
            color: #0D1117;
            font-family: 'JetBrains Mono', monospace;
            user-select: none;
          "
          aria-hidden="true"
        >
          {{ (profile.displayName ?? profile.username).charAt(0).toUpperCase() }}
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0">
          <h1
            class="font-mono font-semibold text-xl mb-0.5"
            style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
          >
            {{ profile.displayName ?? profile.username }}
          </h1>
          <p
            class="text-xs font-mono mb-3"
            style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
          >
            @{{ profile.username }}
          </p>

          <p
            v-if="profile.bio"
            class="text-sm mb-4"
            style="color: #8B949E; line-height: 1.6;"
          >
            {{ profile.bio }}
          </p>

          <!-- Stats row -->
          <div class="flex flex-wrap items-center gap-4">
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 flex-shrink-0" style="color: #8B949E;" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <span class="text-xs font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
                <span style="color: #E6EDF3;">{{ profile.postCount }}</span>
                {{ profile.postCount === 1 ? 'post' : 'posts' }}
              </span>
            </div>
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 flex-shrink-0" style="color: #8B949E;" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span class="text-xs font-mono" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">
                joined <span style="color: #E6EDF3;">{{ formatJoinDate(profile.createdAt) }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Posts section -->
      <p
        class="text-xs font-mono mb-4"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// posts</p>

      <AlertMessage v-if="error" type="error" :message="error" class="mb-4" />

      <!-- Posts loading skeleton -->
      <div
        v-if="postsLoading && !posts"
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
        class="text-center py-16 rounded"
        style="background-color: #161B22; border: 1px solid #30363D;"
      >
        <p class="font-mono text-sm mb-1" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">
          // no posts yet
        </p>
        <p class="text-sm" style="color: #8B949E;">
          {{ profile.displayName ?? profile.username }} hasn't published any posts yet.
        </p>
      </div>

      <!-- Pagination -->
      <div
        v-if="posts && posts.totalPages > 1"
        class="mt-8 flex items-center justify-between"
      >
        <p class="text-xs" style="color: #8B949E;">
          Page {{ posts.number + 1 }} of {{ posts.totalPages }}
        </p>
        <div class="flex gap-2">
          <AppButton
            variant="ghost"
            :disabled="posts.first || postsLoading"
            @click="fetchPosts(currentPage - 1)"
          >
            &larr; Prev
          </AppButton>
          <AppButton
            variant="ghost"
            :disabled="posts.last || postsLoading"
            @click="fetchPosts(currentPage + 1)"
          >
            Next &rarr;
          </AppButton>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* Terminal cursor blink on the avatar block */
.author-avatar {
  position: relative;
  border-right: 3px solid #E6A817;
  animation: cursor-blink 1.1s step-end infinite;
}

@keyframes cursor-blink {
  0%, 100% { border-right-color: #E6A817; }
  50% { border-right-color: transparent; }
}

@media (prefers-reduced-motion: reduce) {
  .author-avatar {
    animation: none;
    border-right-color: #E6A817;
  }
}
</style>
