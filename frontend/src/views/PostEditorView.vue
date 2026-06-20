<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postsApi } from '@/api/posts'
import { useFormValidation, rules } from '@/composables/useFormValidation'
import FormField from '@/components/FormField.vue'
import AppInput from '@/components/AppInput.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import CategorySelect from '@/components/CategorySelect.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { PostResponse, PostStatus } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const route = useRoute()
const router = useRouter()

// Are we editing an existing post?
const postId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})

const isEditing = computed(() => postId.value !== null)

// The post being edited (loaded from /posts/mine/:id)
const loadError = ref<string | null>(null)
const loadingPost = ref(false)

// Form state
const form = reactive({
  title: '',
  contentMarkdown: '',
  excerpt: '',
  categoryId: null as number | null,
})

const currentStatus = ref<PostStatus>('DRAFT')

const { errors, setFieldError, clearErrors, setServerErrors } =
  useFormValidation(form as unknown as Record<string, string>)

const submitError = ref<string | null>(null)
const saving = ref(false)

// Load existing post for edit mode
onMounted(async () => {
  if (!isEditing.value || !postId.value) return

  loadingPost.value = true
  try {
    const p = await postsApi.getMine(postId.value)
    form.title = p.title
    form.contentMarkdown = p.contentMarkdown
    form.excerpt = p.excerpt ?? ''
    form.categoryId = p.category?.id ?? null
    currentStatus.value = p.status
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      loadError.value = data?.message ?? 'Failed to load post for editing.'
    } else {
      loadError.value = 'Failed to load post.'
    }
  } finally {
    loadingPost.value = false
  }
})

function validateForm(): boolean {
  clearErrors()
  let valid = true

  const titleError = rules.required('Title')(form.title)
  if (titleError) {
    setFieldError('title', titleError)
    valid = false
  }

  const contentError = rules.required('Content')(form.contentMarkdown)
  if (contentError) {
    setFieldError('contentMarkdown', contentError)
    valid = false
  }

  if (form.excerpt && form.excerpt.length > 300) {
    setFieldError('excerpt', 'Excerpt must be at most 300 characters')
    valid = false
  }

  return valid
}

async function saveDraft() {
  if (!validateForm()) return

  submitError.value = null
  saving.value = true

  try {
    let result: PostResponse

    if (isEditing.value && postId.value) {
      result = await postsApi.update(postId.value, {
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        excerpt: form.excerpt || undefined,
        categoryId: form.categoryId ?? undefined,
      })
      // If currently published and saving draft, switch to DRAFT
      if (currentStatus.value === 'PUBLISHED') {
        result = await postsApi.changeStatus(postId.value, { status: 'DRAFT' })
      }
    } else {
      result = await postsApi.create({
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        excerpt: form.excerpt || undefined,
        categoryId: form.categoryId ?? undefined,
        status: 'DRAFT',
      })
    }

    currentStatus.value = result.status
    // Navigate to edit view so they can continue editing
    if (!isEditing.value) {
      router.replace(`/posts/${result.id}/edit`)
    }
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) {
        setServerErrors(data.fieldErrors as Record<string, string>)
      } else {
        submitError.value = data?.message ?? 'Failed to save draft.'
      }
    } else {
      submitError.value = 'An unexpected error occurred.'
    }
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!validateForm()) return

  if (!form.categoryId) {
    setFieldError('categoryId', 'A category is required to publish')
    return
  }

  submitError.value = null
  saving.value = true

  try {
    let result: PostResponse

    if (isEditing.value && postId.value) {
      // First update content, then change status
      result = await postsApi.update(postId.value, {
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        excerpt: form.excerpt || undefined,
        categoryId: form.categoryId ?? undefined,
      })
      result = await postsApi.changeStatus(postId.value, { status: 'PUBLISHED' })
    } else {
      result = await postsApi.create({
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        excerpt: form.excerpt || undefined,
        categoryId: form.categoryId ?? undefined,
        status: 'PUBLISHED',
      })
    }

    currentStatus.value = result.status
    if (!isEditing.value) {
      router.replace(`/posts/${result.id}/edit`)
    }
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) {
        setServerErrors(data.fieldErrors as Record<string, string>)
      } else {
        submitError.value = data?.message ?? 'Failed to publish post.'
      }
    } else {
      submitError.value = 'An unexpected error occurred.'
    }
  } finally {
    saving.value = false
  }
}

async function unpublish() {
  if (!isEditing.value || !postId.value) return

  submitError.value = null
  saving.value = true

  try {
    const result = await postsApi.changeStatus(postId.value, { status: 'DRAFT' })
    currentStatus.value = result.status
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      submitError.value = data?.message ?? 'Failed to unpublish post.'
    } else {
      submitError.value = 'An unexpected error occurred.'
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="flex flex-col" style="min-height: calc(100vh - 64px);">
    <!-- Loading existing post -->
    <div
      v-if="loadingPost"
      class="flex items-center justify-center py-20"
      style="color: #8B949E;"
    >
      <svg class="w-6 h-6 animate-spin mr-3" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      Loading post…
    </div>

    <AlertMessage v-else-if="loadError" type="error" :message="loadError" class="m-6" />

    <div v-else class="flex flex-col flex-1">
      <!-- Header area -->
      <div
        class="px-4 sm:px-6 pt-8 pb-6"
        style="border-bottom: 1px solid #30363D;"
      >
        <div class="max-w-7xl mx-auto">
          <!-- Sigil + title row -->
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
            <div>
              <p
                class="text-xs font-mono mb-1"
                style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
                aria-hidden="true"
              >{{ isEditing ? '// editor.edit' : '// editor.new' }}</p>
              <h1
                class="text-xl font-mono font-semibold"
                style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
              >
                {{ isEditing ? 'Edit post' : 'New post' }}
              </h1>
            </div>
            <!-- Status indicator -->
            <div v-if="isEditing" class="flex items-center gap-2">
              <span class="text-xs" style="color: #8B949E;">Status:</span>
              <StatusBadge :status="currentStatus" />
            </div>
          </div>

          <!-- Meta fields: title + category + excerpt -->
          <div class="flex flex-col gap-4 max-w-3xl">
            <FormField id="title" label="Title" :error="errors.title">
              <AppInput
                id="title"
                v-model="form.title"
                type="text"
                placeholder="Post title"
                :has-error="!!errors.title"
                :disabled="saving"
              />
            </FormField>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <FormField
                id="categoryId"
                label="Category"
                :error="errors.categoryId"
                hint="Required to publish"
              >
                <CategorySelect
                  v-model="form.categoryId"
                  :has-error="!!errors.categoryId"
                  :disabled="saving"
                />
              </FormField>

              <FormField
                id="excerpt"
                label="Excerpt"
                :error="errors.excerpt"
                hint="Optional — max 300 characters"
              >
                <AppInput
                  id="excerpt"
                  v-model="form.excerpt"
                  type="text"
                  placeholder="Short description shown in post lists"
                  :has-error="!!errors.excerpt"
                  :disabled="saving"
                />
              </FormField>
            </div>

            <AlertMessage v-if="submitError" type="error" :message="submitError" />
          </div>
        </div>
      </div>

      <!-- Editor pane (fills remaining height) -->
      <div class="flex-1 px-4 sm:px-6 py-4 min-h-0">
        <div
          class="max-w-7xl mx-auto"
          style="height: calc(100vh - 420px); min-height: 300px;"
        >
          <FormField
            id="contentMarkdown"
            label="Content"
            :error="errors.contentMarkdown"
          >
            <div
              style="height: calc(100vh - 460px); min-height: 280px;"
            >
              <MarkdownEditor
                v-model="form.contentMarkdown"
                :disabled="saving"
              />
            </div>
          </FormField>
        </div>
      </div>

      <!-- Action bar -->
      <div
        class="sticky bottom-0 px-4 sm:px-6 py-4"
        style="background-color: #161B22; border-top: 1px solid #30363D;"
      >
        <div class="max-w-7xl mx-auto flex flex-wrap items-center gap-3">
          <AppButton
            variant="secondary"
            :loading="saving"
            :disabled="saving"
            @click="saveDraft"
          >
            Save draft
          </AppButton>

          <AppButton
            variant="primary"
            :loading="saving"
            :disabled="saving"
            @click="publish"
          >
            Publish
          </AppButton>

          <AppButton
            v-if="isEditing && currentStatus === 'PUBLISHED'"
            variant="ghost"
            :loading="saving"
            :disabled="saving"
            @click="unpublish"
          >
            Unpublish
          </AppButton>

          <div class="ml-auto">
            <RouterLink
              to="/me/posts"
              class="text-sm underline"
              style="color: #8B949E;"
            >
              My posts
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
