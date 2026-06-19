<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { usersApi } from '@/api/users'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { UserResponse, Page } from '@/api/types'
import axios from 'axios'
import type { ApiError } from '@/api/types'

const searchQuery = ref('')
const currentPage = ref(0)
const pageSize = 20

const pageData = ref<Page<UserResponse> | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Track per-row saving state
const saving = ref<Record<number, boolean>>({})

async function fetchUsers() {
  loading.value = true
  error.value = null
  try {
    pageData.value = await usersApi.listUsers({
      page: currentPage.value,
      size: pageSize,
      search: searchQuery.value || undefined,
    })
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? 'Failed to load users.'
    } else {
      error.value = 'An unexpected error occurred.'
    }
  } finally {
    loading.value = false
  }
}

// Debounce search
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    fetchUsers()
  }, 350)
})

function goToPage(page: number) {
  currentPage.value = page
  fetchUsers()
}

async function toggleRole(user: UserResponse, role: string) {
  saving.value[user.id] = true
  try {
    const hasRole = user.roles.includes(role)
    const newRoles = hasRole
      ? user.roles.filter((r) => r !== role)
      : [...user.roles, role]

    const updated = await usersApi.updateRoles(user.id, { roles: newRoles })
    // Update in place
    if (pageData.value) {
      const idx = pageData.value.content.findIndex((u) => u.id === user.id)
      if (idx !== -1) pageData.value.content[idx] = updated
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? `Failed to update roles for ${user.username}.`
    }
  } finally {
    saving.value[user.id] = false
  }
}

async function toggleStatus(user: UserResponse) {
  saving.value[user.id] = true
  try {
    const updated = await usersApi.updateStatus(user.id, {
      active: !user.active,
    })
    if (pageData.value) {
      const idx = pageData.value.content.findIndex((u) => u.id === user.id)
      if (idx !== -1) pageData.value.content[idx] = updated
    }
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      error.value = data?.message ?? `Failed to update status for ${user.username}.`
    }
  } finally {
    saving.value[user.id] = false
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// admin.users</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        User management
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Manage roles and account status for all platform users.
      </p>
    </div>

    <!-- Search + stats bar -->
    <div class="flex flex-col sm:flex-row gap-3 mb-6 items-start sm:items-center justify-between">
      <div class="relative w-full sm:w-80">
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
          placeholder="Search users…"
          class="w-full pl-9 pr-3 py-2 rounded text-sm transition-all"
          style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif;"
          aria-label="Search users"
          @focus="($event.target as HTMLInputElement).style.borderColor = '#E6A817'"
          @blur="($event.target as HTMLInputElement).style.borderColor = '#30363D'"
        />
      </div>
      <span class="text-sm shrink-0" style="color: #8B949E;">
        <template v-if="pageData">
          {{ pageData.totalElements }} user{{ pageData.totalElements === 1 ? '' : 's' }}
        </template>
      </span>
    </div>

    <AlertMessage
      v-if="error"
      type="error"
      :message="error"
      class="mb-6"
    />

    <!-- Loading state -->
    <div
      v-if="loading && !pageData"
      class="flex items-center justify-center py-20"
      style="color: #8B949E;"
    >
      <svg class="w-6 h-6 animate-spin mr-3" fill="none" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      Loading users…
    </div>

    <!-- Table -->
    <div
      v-else-if="pageData && pageData.content.length > 0"
      class="rounded-lg overflow-hidden"
      style="border: 1px solid #30363D;"
    >
      <div class="overflow-x-auto">
        <table class="w-full text-sm" style="border-collapse: collapse;">
          <thead>
            <tr style="background-color: #161B22; border-bottom: 1px solid #30363D;">
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">User</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Email</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Roles</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Status</th>
              <th class="text-left px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Joined</th>
              <th class="text-right px-4 py-3 font-mono text-xs tracking-wider" style="color: #8B949E; font-family: 'JetBrains Mono', monospace;">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in pageData.content"
              :key="user.id"
              class="transition-colors"
              style="border-bottom: 1px solid #30363D;"
              :style="{ opacity: saving[user.id] ? 0.6 : 1 }"
              @mouseover="($event.currentTarget as HTMLElement).style.backgroundColor = '#1C2128'"
              @mouseleave="($event.currentTarget as HTMLElement).style.backgroundColor = 'transparent'"
            >
              <!-- User info -->
              <td class="px-4 py-3">
                <div class="flex items-center gap-2.5">
                  <div
                    class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-mono font-semibold flex-shrink-0"
                    style="background-color: #1C2128; color: #E6A817; border: 1px solid #30363D; font-family: 'JetBrains Mono', monospace;"
                  >
                    {{ user.username.charAt(0).toUpperCase() }}
                  </div>
                  <div>
                    <div class="font-mono text-xs" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
                      {{ user.username }}
                    </div>
                    <div v-if="user.displayName" class="text-xs" style="color: #8B949E;">
                      {{ user.displayName }}
                    </div>
                  </div>
                </div>
              </td>

              <!-- Email -->
              <td class="px-4 py-3" style="color: #8B949E;">
                <span class="text-xs">{{ user.email }}</span>
              </td>

              <!-- Roles -->
              <td class="px-4 py-3">
                <div class="flex flex-wrap gap-1.5">
                  <button
                    v-for="role in ['ADMIN', 'USER']"
                    :key="role"
                    type="button"
                    :title="`${user.roles.includes(role) ? 'Remove' : 'Grant'} ${role} role`"
                    class="text-xs px-2 py-0.5 rounded font-mono cursor-pointer transition-all"
                    :style="{
                      fontFamily: '\'JetBrains Mono\', monospace',
                      backgroundColor: user.roles.includes(role)
                        ? (role === 'ADMIN' ? 'rgba(230, 168, 23, 0.2)' : 'rgba(63, 185, 80, 0.2)')
                        : '#1C2128',
                      color: user.roles.includes(role)
                        ? (role === 'ADMIN' ? '#E6A817' : '#3FB950')
                        : '#8B949E',
                      border: `1px solid ${user.roles.includes(role)
                        ? (role === 'ADMIN' ? 'rgba(230, 168, 23, 0.4)' : 'rgba(63, 185, 80, 0.4)')
                        : '#30363D'}`,
                      opacity: saving[user.id] ? 0.5 : 1,
                    }"
                    :disabled="saving[user.id]"
                    @click="toggleRole(user, role)"
                  >
                    {{ role }}
                  </button>
                </div>
              </td>

              <!-- Status -->
              <td class="px-4 py-3">
                <span
                  class="text-xs px-2 py-0.5 rounded font-mono"
                  :style="{
                    fontFamily: '\'JetBrains Mono\', monospace',
                    backgroundColor: user.active ? 'rgba(63, 185, 80, 0.15)' : 'rgba(248, 81, 73, 0.15)',
                    color: user.active ? '#3FB950' : '#F85149',
                    border: `1px solid ${user.active ? 'rgba(63, 185, 80, 0.3)' : 'rgba(248, 81, 73, 0.3)'}`,
                  }"
                >
                  {{ user.active ? 'active' : 'inactive' }}
                </span>
              </td>

              <!-- Joined date -->
              <td class="px-4 py-3 text-xs" style="color: #8B949E;">
                {{ new Date(user.createdAt).toLocaleDateString() }}
              </td>

              <!-- Actions -->
              <td class="px-4 py-3 text-right">
                <AppButton
                  :variant="user.active ? 'danger' : 'secondary'"
                  :disabled="saving[user.id]"
                  :loading="saving[user.id]"
                  class="text-xs px-3 py-1"
                  @click="toggleStatus(user)"
                >
                  {{ user.active ? 'Deactivate' : 'Activate' }}
                </AppButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        v-if="pageData.totalPages > 1"
        class="flex items-center justify-between px-4 py-3"
        style="background-color: #161B22; border-top: 1px solid #30363D;"
      >
        <p class="text-xs" style="color: #8B949E;">
          Page {{ pageData.number + 1 }} of {{ pageData.totalPages }}
        </p>
        <div class="flex gap-2">
          <AppButton
            variant="ghost"
            :disabled="pageData.first || loading"
            class="text-xs px-3 py-1"
            @click="goToPage(currentPage - 1)"
          >
            Previous
          </AppButton>
          <AppButton
            variant="ghost"
            :disabled="pageData.last || loading"
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
      v-else-if="pageData && pageData.content.length === 0"
      class="text-center py-16 rounded-lg"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <p class="text-sm" style="color: #8B949E;">
        {{ searchQuery ? 'No users match your search.' : 'No users found.' }}
      </p>
      <button
        v-if="searchQuery"
        type="button"
        class="mt-3 text-sm cursor-pointer underline"
        style="color: #E6A817; background: transparent; border: none;"
        @click="searchQuery = ''"
      >
        Clear search
      </button>
    </div>
  </div>
</template>
