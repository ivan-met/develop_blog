<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { statsApi } from '@/api/stats'
import AlertMessage from '@/components/AlertMessage.vue'
import type { PlatformStatsResponse } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const stats = ref<PlatformStatsResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function fetchStats() {
  loading.value = true
  error.value = null
  try {
    stats.value = await statsApi.getStats()
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to load platform statistics.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">

    <!-- Page header -->
    <div class="mb-8 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-2">
      <div>
        <p
          class="text-xs font-mono mb-2"
          style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          aria-hidden="true"
        >// admin.dashboard</p>
        <h1
          class="text-2xl font-mono font-semibold"
          style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
        >
          Platform overview
        </h1>
        <p class="text-sm mt-1" style="color: #8B949E;">
          Live snapshot of platform activity and content health.
        </p>
      </div>
      <button
        type="button"
        class="self-start sm:self-auto px-3 py-1.5 rounded text-xs font-mono transition-colors cursor-pointer"
        style="background-color: #1C2128; color: #8B949E; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
        :disabled="loading"
        @click="fetchStats"
        @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
        @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
      >
        {{ loading ? 'Refreshing…' : '↺ Refresh' }}
      </button>
    </div>

    <AlertMessage v-if="error" type="error" :message="error" class="mb-6" />

    <!-- Loading -->
    <div
      v-if="loading && !stats"
      class="flex items-center justify-center py-24"
      style="color: #8B949E;"
    >
      <svg class="w-6 h-6 animate-spin mr-3" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      Loading platform data…
    </div>

    <template v-if="stats">
      <!-- ── Stat cards ──────────────────────────────────────────────────── -->
      <section aria-label="Platform totals" class="mb-8">
        <p class="text-xs font-mono mb-3" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">> totals</p>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">

          <!-- Users -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">users</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.users }}
            </p>
            <p class="text-xs mt-1.5" style="color: #3FB950;">
              {{ stats.totals.activeUsers }} active
            </p>
          </div>

          <!-- Posts -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">posts</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.posts }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">
              <span style="color: #3FB950;">{{ stats.totals.publishedPosts }}</span> published
              &nbsp;·&nbsp;
              <span style="color: #E6A817;">{{ stats.totals.draftPosts }}</span> draft
            </p>
          </div>

          <!-- Comments -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">comments</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.comments }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">platform-wide</p>
          </div>

          <!-- Categories -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">categories</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.categories }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">topic areas</p>
          </div>

          <!-- Likes -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">likes</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.likes }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">total reactions</p>
          </div>

          <!-- Bookmarks -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">bookmarks</p>
            <p class="text-3xl font-mono font-semibold stat-value" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.bookmarks }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">saved by readers</p>
          </div>

          <!-- Published posts (secondary) -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">published</p>
            <p class="text-3xl font-mono font-semibold" style="color: #3FB950; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.publishedPosts }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">live posts</p>
          </div>

          <!-- Draft posts (secondary) -->
          <div
            class="rounded-lg p-4"
            style="background-color: #161B22; border: 1px solid #30363D;"
          >
            <p class="text-xs font-mono mb-2" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">drafts</p>
            <p class="text-3xl font-mono font-semibold" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">
              {{ stats.totals.draftPosts }}
            </p>
            <p class="text-xs mt-1.5" style="color: #8B949E;">awaiting publish</p>
          </div>

        </div>
      </section>

      <!-- ── Ranked lists + recent users ───────────────────────────────── -->
      <section aria-label="Top content and recent signups" class="mb-8">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">

          <!-- Top by views -->
          <div class="rounded-lg overflow-hidden" style="background-color: #161B22; border: 1px solid #30363D;">
            <div class="px-4 py-3" style="border-bottom: 1px solid #30363D;">
              <p class="text-xs font-mono" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">
                // top by views
              </p>
            </div>
            <ol class="divide-y" style="border-color: #30363D;">
              <li
                v-for="(post, idx) in stats.topPostsByViews"
                :key="post.id"
                class="px-4 py-3 flex items-start gap-3"
              >
                <span
                  class="text-xs font-mono flex-shrink-0 w-5 text-right mt-0.5"
                  style="color: #30363D; font-family: 'JetBrains Mono', monospace;"
                >{{ idx + 1 }}</span>
                <div class="min-w-0 flex-1">
                  <RouterLink
                    :to="`/posts/${post.slug}`"
                    class="text-sm font-medium leading-snug block truncate no-underline transition-colors"
                    style="color: #E6EDF3;"
                    @mouseover="($event.currentTarget as HTMLElement).style.color = '#E6A817'"
                    @mouseleave="($event.currentTarget as HTMLElement).style.color = '#E6EDF3'"
                  >{{ post.title }}</RouterLink>
                  <p class="text-xs mt-0.5" style="color: #8B949E;">
                    {{ post.viewCount.toLocaleString() }} views
                    &nbsp;·&nbsp;
                    <span class="font-mono" style="font-family: 'JetBrains Mono', monospace;">@{{ post.author.username }}</span>
                  </p>
                </div>
              </li>
              <li v-if="stats.topPostsByViews.length === 0" class="px-4 py-6 text-center">
                <p class="text-xs" style="color: #8B949E;">No published posts yet.</p>
              </li>
            </ol>
          </div>

          <!-- Top by likes -->
          <div class="rounded-lg overflow-hidden" style="background-color: #161B22; border: 1px solid #30363D;">
            <div class="px-4 py-3" style="border-bottom: 1px solid #30363D;">
              <p class="text-xs font-mono" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">
                // top by likes
              </p>
            </div>
            <ol class="divide-y" style="border-color: #30363D;">
              <li
                v-for="(post, idx) in stats.topPostsByLikes"
                :key="post.slug"
                class="px-4 py-3 flex items-start gap-3"
              >
                <span
                  class="text-xs font-mono flex-shrink-0 w-5 text-right mt-0.5"
                  style="color: #30363D; font-family: 'JetBrains Mono', monospace;"
                >{{ idx + 1 }}</span>
                <div class="min-w-0 flex-1">
                  <RouterLink
                    :to="`/posts/${post.slug}`"
                    class="text-sm font-medium leading-snug block truncate no-underline transition-colors"
                    style="color: #E6EDF3;"
                    @mouseover="($event.currentTarget as HTMLElement).style.color = '#E6A817'"
                    @mouseleave="($event.currentTarget as HTMLElement).style.color = '#E6EDF3'"
                  >{{ post.title }}</RouterLink>
                  <p class="text-xs mt-0.5" style="color: #8B949E;">
                    {{ post.likeCount }} like{{ post.likeCount === 1 ? '' : 's' }}
                    &nbsp;·&nbsp;
                    <span class="font-mono" style="font-family: 'JetBrains Mono', monospace;">@{{ post.author.username }}</span>
                  </p>
                </div>
              </li>
              <li v-if="stats.topPostsByLikes.length === 0" class="px-4 py-6 text-center">
                <p class="text-xs" style="color: #8B949E;">No likes recorded yet.</p>
              </li>
            </ol>
          </div>

          <!-- Recent signups -->
          <div class="rounded-lg overflow-hidden" style="background-color: #161B22; border: 1px solid #30363D;">
            <div class="px-4 py-3" style="border-bottom: 1px solid #30363D;">
              <p class="text-xs font-mono" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">
                // recent signups
              </p>
            </div>
            <ul class="divide-y" style="border-color: #30363D;">
              <li
                v-for="user in stats.recentUsers"
                :key="user.username"
                class="px-4 py-3 flex items-center gap-3"
              >
                <div
                  class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0"
                  style="background-color: #1C2128; color: #E6A817; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
                  aria-hidden="true"
                >
                  {{ user.username.charAt(0).toUpperCase() }}
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-mono truncate" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
                    @{{ user.username }}
                  </p>
                  <p v-if="user.displayName" class="text-xs truncate" style="color: #8B949E;">
                    {{ user.displayName }}
                  </p>
                </div>
                <time
                  class="text-xs flex-shrink-0"
                  :datetime="user.createdAt"
                  style="color: #8B949E;"
                >
                  {{ new Date(user.createdAt).toLocaleDateString() }}
                </time>
              </li>
              <li v-if="stats.recentUsers.length === 0" class="px-4 py-6 text-center">
                <p class="text-xs" style="color: #8B949E;">No users yet.</p>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <!-- ── Admin navigation cards ─────────────────────────────────────── -->
      <section aria-label="Admin areas">
        <p class="text-xs font-mono mb-3" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">> manage</p>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">

          <RouterLink
            to="/admin/users"
            class="rounded-lg p-4 no-underline transition-colors group"
            style="background-color: #161B22; border: 1px solid #30363D;"
            @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
            @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
          >
            <p class="text-xs font-mono mb-2" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">&gt;_ users</p>
            <p class="text-sm font-medium" style="color: #E6EDF3;">User management</p>
            <p class="text-xs mt-1" style="color: #8B949E;">Roles &amp; account status</p>
          </RouterLink>

          <RouterLink
            to="/admin/categories"
            class="rounded-lg p-4 no-underline transition-colors group"
            style="background-color: #161B22; border: 1px solid #30363D;"
            @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
            @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
          >
            <p class="text-xs font-mono mb-2" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">&gt;_ categories</p>
            <p class="text-sm font-medium" style="color: #E6EDF3;">Categories</p>
            <p class="text-xs mt-1" style="color: #8B949E;">Topic taxonomy</p>
          </RouterLink>

          <RouterLink
            to="/admin/comments"
            class="rounded-lg p-4 no-underline transition-colors group"
            style="background-color: #161B22; border: 1px solid #30363D;"
            @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
            @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
          >
            <p class="text-xs font-mono mb-2" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">&gt;_ comments</p>
            <p class="text-sm font-medium" style="color: #E6EDF3;">Comment moderation</p>
            <p class="text-xs mt-1" style="color: #8B949E;">Review &amp; remove comments</p>
          </RouterLink>

          <RouterLink
            to="/admin/posts"
            class="rounded-lg p-4 no-underline transition-colors group"
            style="background-color: #161B22; border: 1px solid #30363D;"
            @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
            @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
          >
            <p class="text-xs font-mono mb-2" style="color: #E6A817; font-family: 'JetBrains Mono', monospace;">&gt;_ content</p>
            <p class="text-sm font-medium" style="color: #E6EDF3;">Content management</p>
            <p class="text-xs mt-1" style="color: #8B949E;">All posts across authors</p>
          </RouterLink>

        </div>
      </section>
    </template>

  </div>
</template>

<style scoped>
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.stat-value::after {
  content: '_';
  display: inline-block;
  margin-left: 2px;
  animation: cursor-blink 1.2s step-end infinite;
  color: #E6A817;
  font-family: 'JetBrains Mono', monospace;
}
</style>
