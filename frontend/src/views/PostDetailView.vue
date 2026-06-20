<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postsApi } from '@/api/posts'
import { commentsApi } from '@/api/comments'
import { useAuthStore } from '@/stores/auth'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import LikeButton from '@/components/LikeButton.vue'
import BookmarkButton from '@/components/BookmarkButton.vue'
import AuthorByline from '@/components/AuthorByline.vue'
import CommentList from '@/components/CommentList.vue'
import CommentForm from '@/components/CommentForm.vue'
import type { PostResponse, CommentResponse } from '@/api/types'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref<PostResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const notFound = ref(false)
const commentListRef = ref<InstanceType<typeof CommentList> | null>(null)
const commentError = ref<string | null>(null)
const commentSubmitting = ref(false)

// Owner: author id matches current user, or current user is admin
const canEdit = ref(false)

function checkCanEdit(p: PostResponse) {
  if (!auth.isAuthenticated) return false
  if (auth.isAdmin) return true
  return auth.currentUser?.id === p.author.id
}

function formatDate(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

onMounted(async () => {
  const slug = route.params.slug as string
  try {
    const data = await postsApi.getBySlug(slug)
    post.value = data
    canEdit.value = checkCanEdit(data)
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      notFound.value = true
    } else {
      error.value = 'Failed to load post.'
    }
  } finally {
    loading.value = false
  }
})

function goEdit() {
  if (post.value) {
    router.push(`/posts/${post.value.id}/edit`)
  }
}

async function handleCommentSubmit(content: string) {
  if (!post.value) return
  commentError.value = null
  commentSubmitting.value = true
  try {
    const newComment: CommentResponse = await commentsApi.create(post.value.slug, { content })
    commentListRef.value?.prependComment(newComment)
  } catch {
    commentError.value = 'Failed to post comment. Please try again.'
  } finally {
    commentSubmitting.value = false
  }
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 py-10">
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
        Post not found
      </h1>
      <p class="text-sm mb-6" style="color: #8B949E;">
        This post may be a draft or may not exist.
      </p>
      <RouterLink
        to="/"
        class="text-sm underline"
        style="color: #E6A817;"
      >
        Back to posts
      </RouterLink>
    </div>

    <!-- Error -->
    <AlertMessage v-else-if="error" type="error" :message="error" />

    <!-- Post -->
    <article v-else-if="post">
      <!-- Sigil + hero title -->
      <header class="mb-8">
        <p
          class="text-xs font-mono mb-3"
          style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          aria-hidden="true"
        >// post.detail</p>

        <h1
          class="font-mono font-semibold mb-5"
          style="
            color: #E6EDF3;
            font-family: 'JetBrains Mono', monospace;
            font-size: clamp(1.5rem, 4vw, 2.25rem);
            line-height: 1.25;
          "
        >
          {{ post.title }}
        </h1>

        <!-- Meta bar -->
        <div class="flex flex-wrap items-center gap-3">
          <!-- Category -->
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

          <!-- Author byline -->
          <AuthorByline :author="post.author" />

          <!-- Published date -->
          <span class="text-sm" style="color: #8B949E;">
            {{ formatDate(post.publishedAt ?? post.createdAt) }}
          </span>

          <!-- Edit button (owner / admin) -->
          <div v-if="canEdit" class="ml-auto">
            <AppButton variant="secondary" @click="goEdit">
              Edit
            </AppButton>
          </div>
        </div>

        <!-- Like + Bookmark row -->
        <div class="flex items-center gap-2 mt-4">
          <LikeButton
            :slug="post.slug"
            :liked="post.liked"
            :like-count="post.likeCount"
            @change="(liked, likeCount) => { if (post) { post.liked = liked; post.likeCount = likeCount } }"
          />
          <BookmarkButton
            :slug="post.slug"
            :bookmarked="post.bookmarked"
            @change="(bookmarked) => { if (post) post.bookmarked = bookmarked }"
          />
        </div>

        <!-- Divider -->
        <div class="mt-6" style="border-top: 1px solid #30363D;" />
      </header>

      <!-- Rendered content -->
      <div class="mt-6">
        <MarkdownPreview :content="post.contentMarkdown" />
      </div>

      <!-- Footer -->
      <div
        class="mt-12 pt-6 flex items-center justify-between"
        style="border-top: 1px solid #30363D;"
      >
        <RouterLink
          to="/"
          class="text-sm underline"
          style="color: #8B949E;"
        >
          &larr; Back to posts
        </RouterLink>
        <AppButton v-if="canEdit" variant="secondary" @click="goEdit">
          Edit post
        </AppButton>
      </div>

      <!-- Discussion section -->
      <section class="mt-12" aria-label="Discussion">
        <div style="border-top: 1px solid #30363D;" class="pt-8">
          <p
            class="text-xs font-mono mb-6"
            style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
            aria-hidden="true"
          >// discussion</p>

          <!-- Comment form -->
          <div class="mb-8">
            <AlertMessage
              v-if="commentError"
              type="error"
              :message="commentError"
              class="mb-3"
            />
            <CommentForm @submitted="handleCommentSubmit" />
          </div>

          <!-- Comment list -->
          <CommentList ref="commentListRef" :slug="post.slug" />
        </div>
      </section>
    </article>
  </div>
</template>
