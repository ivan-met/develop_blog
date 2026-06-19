import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the API modules before importing the store
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
    getMe: vi.fn(),
    updateMe: vi.fn(),
    changePassword: vi.fn(),
    listUsers: vi.fn(),
    getUser: vi.fn(),
    updateRoles: vi.fn(),
    updateStatus: vi.fn(),
  },
}))

import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'

const mockUser = {
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  bio: null,
  avatarUrl: null,
  roles: ['USER'],
  active: true,
  createdAt: '2024-01-01T00:00:00Z',
}

const mockAdminUser = {
  ...mockUser,
  id: 2,
  username: 'adminuser',
  roles: ['ADMIN', 'USER'],
}

const mockAuthResponse = {
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-456',
  user: mockUser,
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
    // Prevent auto-loadMe on store init from interfering
    ;(usersApi.getMe as ReturnType<typeof vi.fn>).mockResolvedValue(mockUser)
  })

  describe('initial state', () => {
    it('has no tokens when localStorage is empty', () => {
      const store = useAuthStore()
      expect(store.accessToken).toBeNull()
      expect(store.refreshToken).toBeNull()
      expect(store.currentUser).toBeNull()
    })

    it('rehydrates tokens from localStorage', () => {
      localStorage.setItem('devblog_access_token', 'stored-token')
      localStorage.setItem('devblog_refresh_token', 'stored-refresh')
      const store = useAuthStore()
      expect(store.accessToken).toBe('stored-token')
      expect(store.refreshToken).toBe('stored-refresh')
    })
  })

  describe('getters', () => {
    it('isAuthenticated returns false when no token', () => {
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(false)
    })

    it('isAuthenticated returns true when token is set', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'test', password: 'pass' })
      expect(store.isAuthenticated).toBe(true)
    })

    it('isAdmin returns false for USER role', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'test', password: 'pass' })
      expect(store.isAdmin).toBe(false)
    })

    it('isAdmin returns true for ADMIN role', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        ...mockAuthResponse,
        user: mockAdminUser,
      })
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'admin', password: 'pass' })
      expect(store.isAdmin).toBe(true)
    })

    it('isUser returns true for USER role', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'test', password: 'pass' })
      expect(store.isUser).toBe(true)
    })

    it('isUser returns false when no user', () => {
      const store = useAuthStore()
      expect(store.isUser).toBe(false)
    })
  })

  describe('login', () => {
    it('sets tokens and user on successful login', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()

      await store.login({ usernameOrEmail: 'testuser', password: 'password123' })

      expect(store.accessToken).toBe('access-token-123')
      expect(store.refreshToken).toBe('refresh-token-456')
      expect(store.currentUser).toEqual(mockUser)
    })

    it('persists tokens to localStorage', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()

      await store.login({ usernameOrEmail: 'testuser', password: 'password123' })

      expect(localStorage.setItem).toHaveBeenCalledWith('devblog_access_token', 'access-token-123')
      expect(localStorage.setItem).toHaveBeenCalledWith('devblog_refresh_token', 'refresh-token-456')
    })

    it('propagates errors from the API', async () => {
      const error = new Error('Invalid credentials')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockRejectedValue(error)
      const store = useAuthStore()

      await expect(store.login({ usernameOrEmail: 'bad', password: 'bad' })).rejects.toThrow()
      expect(store.accessToken).toBeNull()
    })
  })

  describe('register', () => {
    it('sets tokens and user on successful registration', async () => {
      ;(authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      const store = useAuthStore()

      await store.register({ username: 'newuser', email: 'new@example.com', password: 'password123' })

      expect(store.accessToken).toBe('access-token-123')
      expect(store.currentUser).toEqual(mockUser)
    })

    it('propagates registration errors', async () => {
      ;(authApi.register as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Username taken'))
      const store = useAuthStore()

      await expect(
        store.register({ username: 'taken', email: 'x@example.com', password: 'pass123' }),
      ).rejects.toThrow()
    })
  })

  describe('logout', () => {
    it('clears tokens and user from state and localStorage', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      ;(authApi.logout as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)
      const store = useAuthStore()

      await store.login({ usernameOrEmail: 'test', password: 'pass' })
      await store.logout()

      expect(store.accessToken).toBeNull()
      expect(store.refreshToken).toBeNull()
      expect(store.currentUser).toBeNull()
      expect(localStorage.removeItem).toHaveBeenCalledWith('devblog_access_token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('devblog_refresh_token')
    })

    it('calls the logout API with the refresh token', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      ;(authApi.logout as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)
      const store = useAuthStore()

      await store.login({ usernameOrEmail: 'test', password: 'pass' })
      await store.logout()

      // Give the best-effort logout fire-and-forget time to run
      await new Promise((r) => setTimeout(r, 10))
      expect(authApi.logout).toHaveBeenCalledWith({ refreshToken: 'refresh-token-456' })
    })
  })

  describe('refresh', () => {
    it('swaps tokens and returns new access token', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      ;(authApi.refresh as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
      })
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'test', password: 'pass' })

      const newToken = await store.refresh()

      expect(newToken).toBe('new-access-token')
      expect(store.accessToken).toBe('new-access-token')
      expect(store.refreshToken).toBe('new-refresh-token')
    })

    it('throws when no refresh token is available', async () => {
      const store = useAuthStore()
      await expect(store.refresh()).rejects.toThrow('No refresh token available')
    })
  })

  describe('loadMe', () => {
    it('fetches and sets current user', async () => {
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue(mockAuthResponse)
      ;(usersApi.getMe as ReturnType<typeof vi.fn>).mockResolvedValue(mockUser)
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'test', password: 'pass' })

      await store.loadMe()

      expect(usersApi.getMe).toHaveBeenCalled()
      expect(store.currentUser).toEqual(mockUser)
    })

    it('does nothing when not authenticated', async () => {
      ;(usersApi.getMe as ReturnType<typeof vi.fn>).mockResolvedValue(mockUser)
      const store = useAuthStore()

      await store.loadMe()

      // getMe should not be called — may have been called once by init rehydration
      const callsAfterInit = (usersApi.getMe as ReturnType<typeof vi.fn>).mock.calls.length
      // Since no access token, init won't call getMe, and manual loadMe also won't
      expect(callsAfterInit).toBe(0)
    })
  })
})
