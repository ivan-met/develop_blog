<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { engagementApi } from '@/api/engagement'

const props = defineProps<{
  slug: string
  bookmarked: boolean
}>()

const emit = defineEmits<{
  change: [bookmarked: boolean]
}>()

const auth = useAuthStore()
const router = useRouter()

const localBookmarked = ref(props.bookmarked)
const pending = ref(false)

async function toggle() {
  if (!auth.isAuthenticated) {
    router.push('/login')
    return
  }
  if (pending.value) return

  // Optimistic update
  const prev = localBookmarked.value
  localBookmarked.value = !localBookmarked.value
  pending.value = true

  try {
    const result = localBookmarked.value
      ? await engagementApi.bookmark(props.slug)
      : await engagementApi.removeBookmark(props.slug)
    localBookmarked.value = result.bookmarked
    emit('change', result.bookmarked)
  } catch {
    // Revert on error
    localBookmarked.value = prev
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="bookmark-btn inline-flex items-center gap-1.5 px-2 py-1 rounded transition-all cursor-pointer"
    :class="{ 'bookmark-btn--saved': localBookmarked }"
    :aria-label="localBookmarked ? 'Remove bookmark' : 'Bookmark this post'"
    :aria-pressed="localBookmarked"
    :disabled="pending"
    style="background: transparent; border: 1px solid transparent;"
    @click="toggle"
  >
    <!-- Bookmark icon: filled when saved, outline when not -->
    <svg
      class="w-4 h-4 flex-shrink-0 transition-colors"
      viewBox="0 0 24 24"
      aria-hidden="true"
      :fill="localBookmarked ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="2"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"
      />
    </svg>
    <span class="text-xs" style="color: inherit; font-family: 'JetBrains Mono', monospace;">
      {{ localBookmarked ? 'Saved' : 'Save' }}
    </span>
  </button>
</template>

<style scoped>
.bookmark-btn {
  color: #8B949E;
}
.bookmark-btn:hover:not(:disabled) {
  color: #E6A817;
  border-color: rgba(230, 168, 23, 0.25);
  background-color: rgba(230, 168, 23, 0.05);
}
.bookmark-btn--saved {
  color: #E6A817;
}
.bookmark-btn--saved:hover:not(:disabled) {
  color: #B8851A;
  border-color: rgba(184, 133, 26, 0.35);
}
.bookmark-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
