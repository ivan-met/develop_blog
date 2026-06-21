import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
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
  usersApi: { getMe: vi.fn().mockResolvedValue(null) },
}))

const mockStats = {
  totals: {
    users: 10,
    activeUsers: 8,
    posts: 25,
    publishedPosts: 20,
    draftPosts: 5,
    comments: 50,
    categories: 6,
    likes: 120,
    bookmarks: 30,
  },
  topPostsByViews: [
    {
      id: 1,
      slug: 'top-post',
      title: 'Top Post',
      excerpt: null,
      status: 'PUBLISHED' as const,
      category: null,
      author: { id: 1, username: 'alice', displayName: 'Alice' },
      publishedAt: '2025-01-01T00:00:00Z',
      createdAt: '2025-01-01T00:00:00Z',
      tags: [],
      viewCount: 500,
      likeCount: 10,
    },
  ],
  topPostsByLikes: [
    {
      slug: 'liked-post',
      title: 'Liked Post',
      author: { id: 1, username: 'alice', displayName: 'Alice' },
      likeCount: 50,
    },
  ],
  recentUsers: [
    { username: 'newuser', displayName: 'New User', createdAt: '2025-06-01T00:00:00Z' },
  ],
}

describe('statsApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('getStats', () => {
    it('calls GET /admin/stats and returns platform stats', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockStats })

      const { statsApi } = await import('@/api/stats')
      const result = await statsApi.getStats()

      expect(http.get).toHaveBeenCalledWith('/admin/stats')
      expect(result).toEqual(mockStats)
    })

    it('returns totals with expected shape', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockStats })

      const { statsApi } = await import('@/api/stats')
      const result = await statsApi.getStats()

      expect(result.totals.users).toBe(10)
      expect(result.totals.activeUsers).toBe(8)
      expect(result.totals.publishedPosts).toBe(20)
      expect(result.totals.draftPosts).toBe(5)
    })

    it('returns top posts by views list', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockStats })

      const { statsApi } = await import('@/api/stats')
      const result = await statsApi.getStats()

      expect(result.topPostsByViews).toHaveLength(1)
      expect(result.topPostsByViews[0].slug).toBe('top-post')
    })

    it('returns top posts by likes list', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockStats })

      const { statsApi } = await import('@/api/stats')
      const result = await statsApi.getStats()

      expect(result.topPostsByLikes).toHaveLength(1)
      expect(result.topPostsByLikes[0].likeCount).toBe(50)
    })

    it('returns recent users list', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockStats })

      const { statsApi } = await import('@/api/stats')
      const result = await statsApi.getStats()

      expect(result.recentUsers).toHaveLength(1)
      expect(result.recentUsers[0].username).toBe('newuser')
    })
  })
})
