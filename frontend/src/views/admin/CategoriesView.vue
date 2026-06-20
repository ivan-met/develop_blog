<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { categoriesApi } from '@/api/categories'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import FormField from '@/components/FormField.vue'
import AppInput from '@/components/AppInput.vue'
import type { CategoryResponse } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const categories = ref<CategoryResponse[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// Edit/create panel state
type PanelMode = 'idle' | 'create' | 'edit'
const panelMode = ref<PanelMode>('idle')
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
})

const formErrors = reactive({
  name: '',
  description: '',
})

const panelSaving = ref(false)
const deleteInProgress = ref<Record<number, boolean>>({})

// Computed label for the panel
const panelTitle = computed(() =>
  panelMode.value === 'create' ? 'Add category' : 'Edit category',
)

async function fetchCategories() {
  loading.value = true
  error.value = null
  try {
    categories.value = await categoriesApi.list()
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to load categories.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  panelMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.description = ''
  formErrors.name = ''
  formErrors.description = ''
  successMessage.value = null
  error.value = null
}

function openEdit(cat: CategoryResponse) {
  panelMode.value = 'edit'
  editingId.value = cat.id
  form.name = cat.name
  form.description = cat.description ?? ''
  formErrors.name = ''
  formErrors.description = ''
  successMessage.value = null
  error.value = null
}

function closePanel() {
  panelMode.value = 'idle'
  editingId.value = null
}

function validateForm(): boolean {
  formErrors.name = ''
  formErrors.description = ''
  let valid = true

  if (!form.name.trim()) {
    formErrors.name = 'Name is required'
    valid = false
  } else if (form.name.length > 100) {
    formErrors.name = 'Name must be at most 100 characters'
    valid = false
  }

  return valid
}

async function saveCategory() {
  if (!validateForm()) return

  panelSaving.value = true
  successMessage.value = null
  error.value = null

  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
    }

    if (panelMode.value === 'create') {
      const created = await categoriesApi.create(payload)
      categories.value.push(created)
      successMessage.value = `Category "${created.name}" created.`
      closePanel()
    } else if (panelMode.value === 'edit' && editingId.value !== null) {
      const updated = await categoriesApi.update(editingId.value, payload)
      const idx = categories.value.findIndex((c) => c.id === updated.id)
      if (idx !== -1) categories.value[idx] = updated
      successMessage.value = `Category "${updated.name}" updated.`
      closePanel()
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) {
        const fe = data.fieldErrors as Record<string, string>
        formErrors.name = fe.name ?? ''
        formErrors.description = fe.description ?? ''
      } else {
        error.value = data?.message ?? 'Failed to save category.'
      }
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    panelSaving.value = false
  }
}

async function deleteCategory(cat: CategoryResponse) {
  if (!confirm(`Delete category "${cat.name}"? This cannot be undone.`)) return

  deleteInProgress.value[cat.id] = true
  successMessage.value = null
  error.value = null

  try {
    await categoriesApi.remove(cat.id)
    categories.value = categories.value.filter((c) => c.id !== cat.id)
    if (editingId.value === cat.id) closePanel()
    successMessage.value = `Category "${cat.name}" deleted.`
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (err.response?.status === 409) {
        error.value = `Cannot delete "${cat.name}" — it is referenced by existing posts.`
      } else {
        error.value = data?.message ?? 'Failed to delete category.'
      }
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    deleteInProgress.value[cat.id] = false
  }
}

onMounted(fetchCategories)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// admin.categories</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Categories
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Manage the technology categories that organize posts on the platform.
      </p>
    </div>

    <AlertMessage v-if="error" type="error" :message="error" class="mb-6" />
    <AlertMessage v-if="successMessage" type="success" :message="successMessage" class="mb-6" />

    <!-- Main layout: table + side panel -->
    <div class="flex flex-col md:flex-row gap-6">
      <!-- Table panel -->
      <div class="flex-1 min-w-0">
        <!-- Table header bar -->
        <div class="flex items-center justify-between mb-3">
          <span class="text-xs" style="color: #8B949E;">
            {{ categories.length }} categor{{ categories.length === 1 ? 'y' : 'ies' }}
          </span>
          <AppButton
            variant="primary"
            @click="openCreate"
          >
            Add category
          </AppButton>
        </div>

        <!-- Loading -->
        <div
          v-if="loading"
          class="flex items-center justify-center py-16 rounded"
          style="background-color: #161B22; border: 1px solid #30363D; color: #8B949E;"
        >
          <svg class="w-5 h-5 animate-spin mr-2" fill="none" viewBox="0 0 24 24" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          Loading…
        </div>

        <!-- Table -->
        <div
          v-else-if="categories.length > 0"
          class="rounded overflow-hidden"
          style="border: 1px solid #30363D;"
        >
          <table class="w-full text-sm" style="border-collapse: collapse;">
            <thead>
              <tr style="background-color: #161B22; border-bottom: 1px solid #30363D;">
                <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Name</th>
                <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden sm:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Slug</th>
                <th class="text-left px-4 py-3 font-mono text-xs tracking-wider hidden md:table-cell" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Description</th>
                <th class="text-right px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="cat in categories"
                :key="cat.id"
                class="transition-colors"
                style="border-bottom: 1px solid #30363D;"
                :style="{
                  opacity: deleteInProgress[cat.id] ? 0.5 : 1,
                  backgroundColor: editingId === cat.id ? '#1C2128' : 'transparent',
                }"
                @mouseover="($event.currentTarget as HTMLElement).style.backgroundColor = editingId === cat.id ? '#1C2128' : '#161B22'"
                @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = editingId === cat.id ? '#1C2128' : 'transparent'"
              >
                <!-- Name -->
                <td class="px-4 py-3">
                  <span
                    class="font-mono font-medium text-sm"
                    style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
                  >
                    {{ cat.name }}
                  </span>
                </td>

                <!-- Slug -->
                <td class="px-4 py-3 hidden sm:table-cell">
                  <span
                    class="text-xs font-mono"
                    style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
                  >
                    {{ cat.slug }}
                  </span>
                </td>

                <!-- Description -->
                <td class="px-4 py-3 hidden md:table-cell">
                  <span
                    v-if="cat.description"
                    class="text-xs line-clamp-1"
                    style="color: #8B949E;"
                  >
                    {{ cat.description }}
                  </span>
                  <span v-else class="text-xs" style="color: #30363D;">&mdash;</span>
                </td>

                <!-- Actions -->
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-2">
                    <AppButton
                      variant="ghost"
                      :disabled="deleteInProgress[cat.id]"
                      class="text-xs px-2 py-1"
                      @click="openEdit(cat)"
                    >
                      Edit
                    </AppButton>
                    <AppButton
                      variant="danger"
                      :disabled="deleteInProgress[cat.id]"
                      :loading="deleteInProgress[cat.id]"
                      class="text-xs px-2 py-1"
                      @click="deleteCategory(cat)"
                    >
                      Delete
                    </AppButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Empty state -->
        <div
          v-else
          class="text-center py-16 rounded"
          style="background-color: #161B22; border: 1px solid #30363D;"
        >
          <p
            class="font-mono text-sm mb-2"
            style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          >// no categories</p>
          <p class="text-sm mb-4" style="color: #8B949E;">
            Add the first category to let authors categorize their posts.
          </p>
          <AppButton variant="primary" @click="openCreate">
            Add category
          </AppButton>
        </div>
      </div>

      <!-- Side panel: add / edit form -->
      <div
        v-if="panelMode !== 'idle'"
        class="w-full md:w-80 flex-shrink-0"
        style="background-color: #161B22; border: 1px solid #30363D; border-radius: 3px; align-self: flex-start;"
      >
        <div
          class="flex items-center justify-between px-4 py-3"
          style="border-bottom: 1px solid #30363D;"
        >
          <span
            class="text-sm font-mono font-semibold"
            style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
          >{{ panelTitle }}</span>
          <button
            type="button"
            class="p-1 rounded cursor-pointer"
            style="color: #8B949E; background: transparent; border: none;"
            aria-label="Close panel"
            @click="closePanel"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form
          novalidate
          class="p-4 flex flex-col gap-4"
          @submit.prevent="saveCategory"
        >
          <FormField id="cat-name" label="Name" :error="formErrors.name">
            <AppInput
              id="cat-name"
              v-model="form.name"
              type="text"
              placeholder="e.g. Vue.js"
              :has-error="!!formErrors.name"
              :disabled="panelSaving"
            />
          </FormField>

          <FormField id="cat-description" label="Description" :error="formErrors.description" hint="Optional">
            <textarea
              id="cat-description"
              v-model="form.description"
              rows="3"
              placeholder="A short description of this category"
              class="w-full px-3 py-2.5 rounded text-sm resize-y transition-all"
              style="background-color: #0D1117; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif; min-height: 72px;"
              :disabled="panelSaving"
              @focus="($event.target as HTMLTextAreaElement).style.borderColor = '#E6A817'"
              @blur="($event.target as HTMLTextAreaElement).style.borderColor = '#30363D'"
            />
          </FormField>

          <div class="flex gap-2">
            <AppButton
              type="submit"
              variant="primary"
              :loading="panelSaving"
              :disabled="panelSaving"
            >
              {{ panelMode === 'create' ? 'Create' : 'Save' }}
            </AppButton>
            <AppButton
              type="button"
              variant="ghost"
              :disabled="panelSaving"
              @click="closePanel"
            >
              Cancel
            </AppButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
