<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { engagementApi } from '@/api/engagement'

const props = defineProps<{
  slug: string
  liked: boolean
  likeCount: number
}>()

const emit = defineEmits<{
  change: [liked: boolean, likeCount: number]
}>()

const auth = useAuthStore()
const router = useRouter()

const localLiked = ref(props.liked)
const localCount = ref(props.likeCount)
const pending = ref(false)

async function toggle() {
  if (!auth.isAuthenticated) {
    router.push('/login')
    return
  }
  if (pending.value) return

  // Optimistic update
  const prevLiked = localLiked.value
  const prevCount = localCount.value
  localLiked.value = !localLiked.value
  localCount.value = localLiked.value ? prevCount + 1 : prevCount - 1
  pending.value = true

  try {
    const result = localLiked.value
      ? await engagementApi.like(props.slug)
      : await engagementApi.unlike(props.slug)
    localLiked.value = result.liked
    localCount.value = result.likeCount
    emit('change', result.liked, result.likeCount)
  } catch {
    // Revert on error
    localLiked.value = prevLiked
    localCount.value = prevCount
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <button
    type="button"
    class="like-btn inline-flex items-center gap-1.5 px-2 py-1 rounded transition-all cursor-pointer"
    :class="{ 'like-btn--liked': localLiked }"
    :aria-label="localLiked ? 'Unlike this post' : 'Like this post'"
    :aria-pressed="localLiked"
    :disabled="pending"
    style="background: transparent; border: 1px solid transparent;"
    @click="toggle"
  >
    <!-- Heart icon: filled when liked, outline when not -->
    <svg
      class="w-4 h-4 flex-shrink-0 transition-colors"
      viewBox="0 0 24 24"
      aria-hidden="true"
      :fill="localLiked ? 'currentColor' : 'none'"
      stroke="currentColor"
      stroke-width="2"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
      />
    </svg>
    <span
      class="text-xs font-mono tabular-nums"
      style="font-family: 'JetBrains Mono', monospace;"
    >
      {{ localCount }}
    </span>
  </button>
</template>

<style scoped>
.like-btn {
  color: #8B949E;
}
.like-btn:hover:not(:disabled) {
  color: #E6A817;
  border-color: rgba(230, 168, 23, 0.25);
  background-color: rgba(230, 168, 23, 0.05);
}
.like-btn--liked {
  color: #E6A817;
}
.like-btn--liked:hover:not(:disabled) {
  color: #B8851A;
  border-color: rgba(184, 133, 26, 0.35);
}
.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
