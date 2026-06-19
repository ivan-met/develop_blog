import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, redirectIfAuth: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true, redirectIfAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/admin/UsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
  ],
})

// ─── Global navigation guard ─────────────────────────────────────────────────
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Redirect authenticated users away from public-only pages (login/register)
  if (to.meta.redirectIfAuth && auth.isAuthenticated) {
    return { name: 'profile' }
  }

  // Protect authenticated routes
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // Protect admin-only routes
  if (to.meta.requiresAdmin) {
    // Wait for user to be loaded if we have a token but no user yet
    if (auth.isAuthenticated && !auth.currentUser) {
      await auth.loadMe()
    }
    if (!auth.isAdmin) {
      return { name: 'profile' }
    }
  }

  return true
})

export default router
