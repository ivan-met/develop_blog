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
  },
}))

// Minimal router mirroring the blog-related routes and guard logic
function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/',
        component: { template: '<div>Home</div>' },
        meta: { public: true },
      },
      {
        path: '/posts/:slug',
        component: { template: '<div>Post</div>' },
        meta: { public: true },
      },
      {
        path: '/login',
        component: { template: '<div>Login</div>' },
        meta: { public: true, redirectIfAuth: true },
      },
      {
        path: '/profile',
        component: { template: '<div>Profile</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/posts/new',
        component: { template: '<div>New Post</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/posts/:id/edit',
        component: { template: '<div>Edit Post</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/me/posts',
        component: { template: '<div>My Posts</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/admin/categories',
        component: { template: '<div>Categories</div>' },
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  })

  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.meta.redirectIfAuth && auth.isAuthenticated) {
      return { path: '/' }
    }

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (to.meta.requiresAdmin) {
      if (auth.isAuthenticated && !auth.currentUser) {
        await auth.loadMe()
      }
      if (!auth.isAdmin) {
        return { path: '/' }
      }
    }

    return true
  })

  return router
}

async function loginAsUser(store: ReturnType<typeof useAuthStore>) {
  const { authApi } = await import('@/api/auth')
  ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
    accessToken: 'user-token',
    refreshToken: 'user-refresh',
    user: {
      id: 1,
      username: 'alice',
      email: 'alice@test.com',
      displayName: null,
      bio: null,
      avatarUrl: null,
      roles: ['USER'],
      active: true,
      createdAt: '2024-01-01',
    },
  })
  await store.login({ usernameOrEmail: 'alice', password: 'pass' })
}

async function loginAsAdmin(store: ReturnType<typeof useAuthStore>) {
  const { authApi } = await import('@/api/auth')
  ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
    accessToken: 'admin-token',
    refreshToken: 'admin-refresh',
    user: {
      id: 2,
      username: 'admin',
      email: 'admin@test.com',
      displayName: null,
      bio: null,
      avatarUrl: null,
      roles: ['ADMIN'],
      active: true,
      createdAt: '2024-01-01',
    },
  })
  await store.login({ usernameOrEmail: 'admin', password: 'adminpass' })
}

describe('blog route guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('public routes — unauthenticated access', () => {
    it('allows access to / (home)', async () => {
      const router = createTestRouter()
      await router.push('/')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('allows access to /posts/:slug (post detail)', async () => {
      const router = createTestRouter()
      await router.push('/posts/my-great-post')
      expect(router.currentRoute.value.path).toBe('/posts/my-great-post')
    })
  })

  describe('/posts/new — requiresAuth', () => {
    it('redirects unauthenticated user to /login', async () => {
      const router = createTestRouter()
      await router.push('/posts/new')
      expect(router.currentRoute.value.path).toBe('/login')
      expect(router.currentRoute.value.query.redirect).toBe('/posts/new')
    })

    it('allows authenticated USER to access /posts/new', async () => {
      const store = useAuthStore()
      await loginAsUser(store)
      const router = createTestRouter()
      await router.push('/posts/new')
      expect(router.currentRoute.value.path).toBe('/posts/new')
    })

    it('allows authenticated ADMIN to access /posts/new (guard does not block — authoring restriction is backend-side)', async () => {
      const store = useAuthStore()
      await loginAsAdmin(store)
      const router = createTestRouter()
      await router.push('/posts/new')
      // Route is requiresAuth only, not requiresAdmin — admin is authenticated so can pass
      expect(router.currentRoute.value.path).toBe('/posts/new')
    })
  })

  describe('/posts/:id/edit — requiresAuth', () => {
    it('redirects unauthenticated user to /login', async () => {
      const router = createTestRouter()
      await router.push('/posts/42/edit')
      expect(router.currentRoute.value.path).toBe('/login')
      expect(router.currentRoute.value.query.redirect).toBe('/posts/42/edit')
    })

    it('allows authenticated user to access /posts/:id/edit', async () => {
      const store = useAuthStore()
      await loginAsUser(store)
      const router = createTestRouter()
      await router.push('/posts/42/edit')
      expect(router.currentRoute.value.path).toBe('/posts/42/edit')
    })
  })

  describe('/me/posts — requiresAuth', () => {
    it('redirects unauthenticated user to /login', async () => {
      const router = createTestRouter()
      await router.push('/me/posts')
      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('allows authenticated USER', async () => {
      const store = useAuthStore()
      await loginAsUser(store)
      const router = createTestRouter()
      await router.push('/me/posts')
      expect(router.currentRoute.value.path).toBe('/me/posts')
    })
  })

  describe('/admin/categories — requiresAdmin', () => {
    it('redirects unauthenticated user to /login', async () => {
      const router = createTestRouter()
      await router.push('/admin/categories')
      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('redirects authenticated USER (non-admin) to /', async () => {
      const store = useAuthStore()
      await loginAsUser(store)
      const router = createTestRouter()
      await router.push('/admin/categories')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('allows authenticated ADMIN', async () => {
      const store = useAuthStore()
      await loginAsAdmin(store)
      const router = createTestRouter()
      await router.push('/admin/categories')
      expect(router.currentRoute.value.path).toBe('/admin/categories')
    })
  })
})
