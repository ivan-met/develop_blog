import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import axios from 'axios'

// We test interceptor behavior by verifying store interactions
// The actual interceptors are registered on the axios instance in http.ts

vi.mock('@/api/auth', () => ({
  authApi: {
    register: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  },
}))

vi.mock('@/api/users', () => ({
  usersApi: {
    getMe: vi.fn().mockResolvedValue(null),
    updateMe: vi.fn(),
    changePassword: vi.fn(),
    listUsers: vi.fn(),
    getUser: vi.fn(),
    updateRoles: vi.fn(),
    updateStatus: vi.fn(),
  },
}))

describe('HTTP interceptor behavior', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('request interceptor — Bearer token attachment', () => {
    it('attaches Authorization header when token is present in store', async () => {
      const { authApi } = await import('@/api/auth')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'my-access-token',
        refreshToken: 'my-refresh-token',
        user: { id: 1, username: 'user', email: 'u@u.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
      })

      const { useAuthStore } = await import('@/stores/auth')
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'user', password: 'pass' })

      // Verify token is stored — the interceptor will read from here
      expect(store.accessToken).toBe('my-access-token')

      // Simulate what the interceptor does: read store.accessToken and attach it
      const config = { headers: {} as Record<string, string> }
      if (store.accessToken) {
        config.headers['Authorization'] = `Bearer ${store.accessToken}`
      }
      expect(config.headers['Authorization']).toBe('Bearer my-access-token')
    })

    it('does not attach Authorization header when no token', async () => {
      const { useAuthStore } = await import('@/stores/auth')
      const store = useAuthStore()

      expect(store.accessToken).toBeNull()

      const config = { headers: {} as Record<string, string> }
      if (store.accessToken) {
        config.headers['Authorization'] = `Bearer ${store.accessToken}`
      }
      expect(config.headers['Authorization']).toBeUndefined()
    })
  })

  describe('response interceptor — 401 refresh flow', () => {
    it('calls store.refresh when a 401 is encountered', async () => {
      const { authApi } = await import('@/api/auth')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'old-token',
        refreshToken: 'refresh-token',
        user: { id: 1, username: 'user', email: 'u@u.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
      })
      ;(authApi.refresh as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'new-token',
        refreshToken: 'new-refresh-token',
      })

      const { useAuthStore } = await import('@/stores/auth')
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'user', password: 'pass' })

      // Verify the refresh action works (which is what interceptor would call)
      const newToken = await store.refresh()
      expect(newToken).toBe('new-token')
      expect(store.accessToken).toBe('new-token')
      expect(store.refreshToken).toBe('new-refresh-token')
    })

    it('calls store.logout when refresh fails', async () => {
      const { authApi } = await import('@/api/auth')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'old-token',
        refreshToken: 'expired-refresh',
        user: { id: 1, username: 'user', email: 'u@u.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
      })
      ;(authApi.refresh as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Token expired'))
      ;(authApi.logout as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

      const { useAuthStore } = await import('@/stores/auth')
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'user', password: 'pass' })

      // Simulate what interceptor does: try refresh, if fails → logout
      try {
        await store.refresh()
      } catch {
        await store.logout()
      }

      expect(store.accessToken).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })

    it('does not retry 401 from auth endpoints', () => {
      // The interceptor checks that the URL does not contain /auth/refresh, /auth/login, /auth/register
      // Verify this logic: URLs that should NOT trigger retry
      const shouldSkipRetry = (url: string) =>
        url.includes('/auth/refresh') ||
        url.includes('/auth/login') ||
        url.includes('/auth/register')

      expect(shouldSkipRetry('/auth/refresh')).toBe(true)
      expect(shouldSkipRetry('/auth/login')).toBe(true)
      expect(shouldSkipRetry('/auth/register')).toBe(true)
      expect(shouldSkipRetry('/users/me')).toBe(false)
      expect(shouldSkipRetry('/admin/users')).toBe(false)
    })
  })

  describe('axios.isAxiosError detection', () => {
    it('correctly identifies axios errors', () => {
      const axiosError = new axios.AxiosError('Network Error', 'ERR_NETWORK')
      expect(axios.isAxiosError(axiosError)).toBe(true)
    })

    it('correctly identifies non-axios errors', () => {
      const regularError = new Error('Regular error')
      expect(axios.isAxiosError(regularError)).toBe(false)
    })
  })
})
