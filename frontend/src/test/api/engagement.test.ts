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

const mockPostSummary = {
  id: 1,
  slug: 'hello-world',
  title: 'Hello World',
  excerpt: 'Intro',
  status: 'PUBLISHED' as const,
  category: null,
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: '2025-01-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: [],
  viewCount: 5,
  likeCount: 2,
}

const mockPage = {
  content: [mockPostSummary],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 12,
  first: true,
  last: true,
}

describe('engagementApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('like', () => {
    it('calls POST /posts/:slug/like and returns LikeResponse', async () => {
      const http = (await import('@/api/http')).default
      const likeResponse = { likeCount: 5, liked: true }
      ;(http.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: likeResponse })

      const { engagementApi } = await import('@/api/engagement')
      const result = await engagementApi.like('my-post')

      expect(http.post).toHaveBeenCalledWith('/posts/my-post/like')
      expect(result).toEqual(likeResponse)
    })
  })

  describe('unlike', () => {
    it('calls DELETE /posts/:slug/like and returns LikeResponse', async () => {
      const http = (await import('@/api/http')).default
      const likeResponse = { likeCount: 4, liked: false }
      ;(http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({ data: likeResponse })

      const { engagementApi } = await import('@/api/engagement')
      const result = await engagementApi.unlike('my-post')

      expect(http.delete).toHaveBeenCalledWith('/posts/my-post/like')
      expect(result).toEqual(likeResponse)
    })
  })

  describe('bookmark', () => {
    it('calls POST /posts/:slug/bookmark and returns BookmarkResponse', async () => {
      const http = (await import('@/api/http')).default
      ;(http.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { bookmarked: true } })

      const { engagementApi } = await import('@/api/engagement')
      const result = await engagementApi.bookmark('my-post')

      expect(http.post).toHaveBeenCalledWith('/posts/my-post/bookmark')
      expect(result).toEqual({ bookmarked: true })
    })
  })

  describe('removeBookmark', () => {
    it('calls DELETE /posts/:slug/bookmark and returns BookmarkResponse', async () => {
      const http = (await import('@/api/http')).default
      ;(http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { bookmarked: false } })

      const { engagementApi } = await import('@/api/engagement')
      const result = await engagementApi.removeBookmark('my-post')

      expect(http.delete).toHaveBeenCalledWith('/posts/my-post/bookmark')
      expect(result).toEqual({ bookmarked: false })
    })
  })

  describe('listMyBookmarks', () => {
    it('calls GET /users/me/bookmarks with page and size params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { engagementApi } = await import('@/api/engagement')
      const result = await engagementApi.listMyBookmarks({ page: 0, size: 12 })

      expect(http.get).toHaveBeenCalledWith('/users/me/bookmarks', {
        params: { page: 0, size: 12 },
      })
      expect(result).toEqual(mockPage)
    })

    it('works without optional params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { engagementApi } = await import('@/api/engagement')
      await engagementApi.listMyBookmarks({})

      expect(http.get).toHaveBeenCalledWith('/users/me/bookmarks', {
        params: {},
      })
    })
  })
})
