import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import type { UserResponse, RegisterRequest, LoginRequest } from '@/api/types'

const ACCESS_TOKEN_KEY = 'devblog_access_token'
const REFRESH_TOKEN_KEY = 'devblog_refresh_token'

export const useAuthStore = defineStore('auth', () => {
  // ─── State ─────────────────────────────────────────────────────────────────
  const accessToken = ref<string | null>(
    localStorage.getItem(ACCESS_TOKEN_KEY),
  )
  const refreshToken = ref<string | null>(
    localStorage.getItem(REFRESH_TOKEN_KEY),
  )
  const currentUser = ref<UserResponse | null>(null)
  const loading = ref(false)

  // ─── Getters ───────────────────────────────────────────────────────────────
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() =>
    currentUser.value?.roles.includes('ADMIN') ?? false,
  )
  const isUser = computed(() =>
    currentUser.value?.roles.includes('USER') ?? false,
  )

  // ─── Helpers ───────────────────────────────────────────────────────────────
  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem(ACCESS_TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  }

  function clearTokens() {
    accessToken.value = null
    refreshToken.value = null
    currentUser.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  // ─── Actions ───────────────────────────────────────────────────────────────
  async function register(data: RegisterRequest): Promise<void> {
    loading.value = true
    try {
      const response = await authApi.register(data)
      setTokens(response.accessToken, response.refreshToken)
      currentUser.value = response.user
    } finally {
      loading.value = false
    }
  }

  async function login(data: LoginRequest): Promise<void> {
    loading.value = true
    try {
      const response = await authApi.login(data)
      setTokens(response.accessToken, response.refreshToken)
      currentUser.value = response.user
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    const token = refreshToken.value
    clearTokens()
    if (token) {
      // Best-effort: don't block UI on this
      authApi.logout({ refreshToken: token }).catch(() => {
        // Ignore logout errors — tokens are already cleared locally
      })
    }
  }

  async function refresh(): Promise<string> {
    const token = refreshToken.value
    if (!token) throw new Error('No refresh token available')

    const response = await authApi.refresh({ refreshToken: token })
    setTokens(response.accessToken, response.refreshToken)
    return response.accessToken
  }

  async function loadMe(): Promise<void> {
    if (!accessToken.value) return
    loading.value = true
    try {
      currentUser.value = await usersApi.getMe()
    } catch {
      // If we can't load the user (e.g. expired token), keep existing state
      // The 401 interceptor will handle token refresh if applicable
    } finally {
      loading.value = false
    }
  }

  // ─── Rehydrate on init ─────────────────────────────────────────────────────
  // If there's a stored token, load the current user's profile
  if (accessToken.value && !currentUser.value) {
    loadMe()
  }

  return {
    // State
    accessToken,
    refreshToken,
    currentUser,
    loading,
    // Getters
    isAuthenticated,
    isAdmin,
    isUser,
    // Actions
    register,
    login,
    logout,
    refresh,
    loadMe,
  }
})
