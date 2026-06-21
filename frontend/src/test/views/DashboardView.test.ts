import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardView from '@/views/admin/DashboardView.vue'
import type { PlatformStatsResponse } from '@/api/types'

vi.mock('@/api/stats', () => ({
  statsApi: {
    getStats: vi.fn(),
  },
}))

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

const mockStats: PlatformStatsResponse = {
  totals: {
    users: 42,
    activeUsers: 38,
    posts: 100,
    publishedPosts: 80,
    draftPosts: 20,
    comments: 250,
    categories: 8,
    likes: 560,
    bookmarks: 130,
  },
  topPostsByViews: [
    {
      id: 1,
      slug: 'most-viewed',
      title: 'Most Viewed Post',
      excerpt: null,
      status: 'PUBLISHED',
      category: { id: 1, name: 'Vue', slug: 'vue', description: null },
      author: { id: 1, username: 'alice', displayName: 'Alice' },
      publishedAt: '2025-01-01T00:00:00Z',
      createdAt: '2025-01-01T00:00:00Z',
      tags: [],
      viewCount: 9000,
      likeCount: 50,
    },
  ],
  topPostsByLikes: [
    {
      slug: 'most-liked',
      title: 'Most Liked Post',
      author: { id: 2, username: 'bob', displayName: 'Bob' },
      likeCount: 200,
    },
  ],
  recentUsers: [
    { username: 'charlie', displayName: 'Charlie Brown', createdAt: '2025-06-01T00:00:00Z' },
    { username: 'diana', displayName: null, createdAt: '2025-05-28T00:00:00Z' },
  ],
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin', component: DashboardView },
      { path: '/posts/:slug', component: { template: '<div>Post</div>' } },
      { path: '/admin/users', component: { template: '<div>Users</div>' } },
      { path: '/admin/categories', component: { template: '<div>Categories</div>' } },
      { path: '/admin/comments', component: { template: '<div>Comments</div>' } },
      { path: '/admin/posts', component: { template: '<div>Content</div>' } },
    ],
  })
}

async function mountDashboard() {
  const router = createTestRouter()
  await router.push('/admin')
  const wrapper = mount(DashboardView, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('DashboardView (admin)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders page heading', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('Platform overview')
  })

  it('renders the admin.dashboard eyebrow label', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('// admin.dashboard')
  })

  it('displays total user count', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('38 active')
  })

  it('displays post totals', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('100')
  })

  it('displays comment count', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('250')
  })

  it('displays category count', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('8')
  })

  it('renders top post by views', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('Most Viewed Post')
  })

  it('renders top post by likes', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('Most Liked Post')
  })

  it('renders recent signups', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('@charlie')
    expect(wrapper.text()).toContain('@diana')
  })

  it('renders navigation cards to all admin areas', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue(mockStats)

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('User management')
    expect(wrapper.text()).toContain('Comment moderation')
    expect(wrapper.text()).toContain('Content management')
  })

  it('shows loading spinner before data arrives', async () => {
    const { statsApi } = await import('@/api/stats')
    // Never resolves during this check
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockReturnValue(new Promise(() => {}))

    const router = createTestRouter()
    await router.push('/admin')
    const wrapper = mount(DashboardView, {
      global: { plugins: [router] },
    })
    // nextTick processes onMounted which sets loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Loading platform data')
  })

  it('shows error message when stats fetch fails', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Network error'))

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('unexpected error')
  })

  it('shows empty state messages when lists are empty', async () => {
    const { statsApi } = await import('@/api/stats')
    ;(statsApi.getStats as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockStats,
      topPostsByViews: [],
      topPostsByLikes: [],
      recentUsers: [],
    })

    const { wrapper } = await mountDashboard()
    expect(wrapper.text()).toContain('No published posts yet')
    expect(wrapper.text()).toContain('No likes recorded yet')
    expect(wrapper.text()).toContain('No users yet')
  })
})
