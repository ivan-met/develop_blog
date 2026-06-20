import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ─── Public ────────────────────────────────────────────────────────────
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/PostsListView.vue'),
      meta: { public: true },
    },

    // ─── Auth pages ─────────────────────────────────────────────────────────
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

    // ─── Authenticated (post routes before :slug to prevent conflict) ────────
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts/new',
      name: 'post-new',
      component: () => import('@/views/PostEditorView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts/:id/edit',
      name: 'post-edit',
      component: () => import('@/views/PostEditorView.vue'),
      meta: { requiresAuth: true },
    },

    // ─── Public post detail (after static /posts/* to avoid conflicts) ────────
    {
      path: '/posts/:slug',
      name: 'post-detail',
      component: () => import('@/views/PostDetailView.vue'),
      meta: { public: true },
    },
    {
      path: '/me/posts',
      name: 'my-posts',
      component: () => import('@/views/MyPostsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/me/bookmarks',
      name: 'bookmarks',
      component: () => import('@/views/BookmarksView.vue'),
      meta: { requiresAuth: true },
    },

    // ─── Public author profiles ─────────────────────────────────────────────
    {
      path: '/authors/:username',
      name: 'author-profile',
      component: () => import('@/views/AuthorProfileView.vue'),
      meta: { public: true },
    },

    // ─── Admin ─────────────────────────────────────────────────────────────
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/admin/UsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/categories',
      name: 'admin-categories',
      component: () => import('@/views/admin/CategoriesView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },

    // ─── Fallback ───────────────────────────────────────────────────────────
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

// ─── Global navigation guard ─────────────────────────────────────────────────
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Redirect authenticated users away from public-only pages (login/register)
  if (to.meta.redirectIfAuth && auth.isAuthenticated) {
    return { name: 'home' }
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
      return { name: 'home' }
    }
  }

  return true
})

export default router
