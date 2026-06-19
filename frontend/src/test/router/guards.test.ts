import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

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

// Create a test router that mirrors the production guard logic
function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/login' },
      {
        path: '/login',
        component: { template: '<div>Login</div>' },
        meta: { public: true, redirectIfAuth: true },
      },
      {
        path: '/register',
        component: { template: '<div>Register</div>' },
        meta: { public: true, redirectIfAuth: true },
      },
      {
        path: '/profile',
        component: { template: '<div>Profile</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/admin/users',
        component: { template: '<div>Admin</div>' },
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  })

  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.meta.redirectIfAuth && auth.isAuthenticated) {
      return { path: '/profile' }
    }

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (to.meta.requiresAdmin) {
      if (auth.isAuthenticated && !auth.currentUser) {
        await auth.loadMe()
      }
      if (!auth.isAdmin) {
        return { path: '/profile' }
      }
    }

    return true
  })

  return router
}

async function navigateTo(router: ReturnType<typeof createTestRouter>, path: string) {
  await router.push(path)
  return router.currentRoute.value
}

describe('route guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('unauthenticated user', () => {
    it('can access /login', async () => {
      const router = createTestRouter()
      const route = await navigateTo(router, '/login')
      expect(route.path).toBe('/login')
    })

    it('can access /register', async () => {
      const router = createTestRouter()
      const route = await navigateTo(router, '/register')
      expect(route.path).toBe('/register')
    })

    it('is redirected from /profile to /login', async () => {
      const router = createTestRouter()
      const route = await navigateTo(router, '/profile')
      expect(route.path).toBe('/login')
    })

    it('is redirected from /admin/users to /login', async () => {
      const router = createTestRouter()
      const route = await navigateTo(router, '/admin/users')
      expect(route.path).toBe('/login')
    })

    it('includes the intended path as redirect query param', async () => {
      const router = createTestRouter()
      const route = await navigateTo(router, '/profile')
      expect(route.query.redirect).toBe('/profile')
    })
  })

  describe('authenticated non-admin user', () => {
    async function loginAsUser() {
      const { authApi } = await import('@/api/auth')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'token',
        refreshToken: 'refresh',
        user: { id: 1, username: 'user', email: 'user@test.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
      })
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'user', password: 'pass' })
    }

    it('can access /profile', async () => {
      await loginAsUser()
      const router = createTestRouter()
      const route = await navigateTo(router, '/profile')
      expect(route.path).toBe('/profile')
    })

    it('is redirected from /admin/users to /profile', async () => {
      await loginAsUser()
      const router = createTestRouter()
      const route = await navigateTo(router, '/admin/users')
      expect(route.path).toBe('/profile')
    })

    it('is redirected away from /login to /profile (already authenticated)', async () => {
      await loginAsUser()
      const router = createTestRouter()
      const route = await navigateTo(router, '/login')
      expect(route.path).toBe('/profile')
    })

    it('is redirected away from /register to /profile (already authenticated)', async () => {
      await loginAsUser()
      const router = createTestRouter()
      const route = await navigateTo(router, '/register')
      expect(route.path).toBe('/profile')
    })
  })

  describe('authenticated admin user', () => {
    async function loginAsAdmin() {
      const { authApi } = await import('@/api/auth')
      ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
        accessToken: 'admin-token',
        refreshToken: 'admin-refresh',
        user: { id: 2, username: 'admin', email: 'admin@test.com', displayName: null, bio: null, avatarUrl: null, roles: ['ADMIN', 'USER'], active: true, createdAt: '2024-01-01' },
      })
      const store = useAuthStore()
      await store.login({ usernameOrEmail: 'admin', password: 'adminpass' })
    }

    it('can access /admin/users', async () => {
      await loginAsAdmin()
      const router = createTestRouter()
      const route = await navigateTo(router, '/admin/users')
      expect(route.path).toBe('/admin/users')
    })

    it('can access /profile', async () => {
      await loginAsAdmin()
      const router = createTestRouter()
      const route = await navigateTo(router, '/profile')
      expect(route.path).toBe('/profile')
    })
  })
})
