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

const mockComment = {
  id: 1,
  content: 'Great post!',
  author: { id: 2, username: 'bob', displayName: 'Bob' },
  createdAt: '2025-01-01T00:00:00Z',
  canDelete: false,
}

const mockPage = {
  content: [mockComment],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 10,
  first: true,
  last: true,
}

describe('commentsApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('list', () => {
    it('calls GET /posts/:slug/comments with page and size params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { commentsApi } = await import('@/api/comments')
      const result = await commentsApi.list('my-post', { page: 0, size: 10 })

      expect(http.get).toHaveBeenCalledWith('/posts/my-post/comments', {
        params: { page: 0, size: 10 },
      })
      expect(result).toEqual(mockPage)
    })

    it('works without optional params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { commentsApi } = await import('@/api/comments')
      await commentsApi.list('my-post', {})

      expect(http.get).toHaveBeenCalledWith('/posts/my-post/comments', {
        params: {},
      })
    })
  })

  describe('create', () => {
    it('calls POST /posts/:slug/comments with content body', async () => {
      const http = (await import('@/api/http')).default
      ;(http.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockComment })

      const { commentsApi } = await import('@/api/comments')
      const result = await commentsApi.create('my-post', { content: 'Great post!' })

      expect(http.post).toHaveBeenCalledWith('/posts/my-post/comments', {
        content: 'Great post!',
      })
      expect(result).toEqual(mockComment)
    })
  })

  describe('remove', () => {
    it('calls DELETE /comments/:id and returns void', async () => {
      const http = (await import('@/api/http')).default
      ;(http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({})

      const { commentsApi } = await import('@/api/comments')
      const result = await commentsApi.remove(42)

      expect(http.delete).toHaveBeenCalledWith('/comments/42')
      expect(result).toBeUndefined()
    })
  })

  describe('listAll (admin)', () => {
    it('calls GET /admin/comments with search and pagination params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { commentsApi } = await import('@/api/comments')
      const result = await commentsApi.listAll({ search: 'hello', page: 0, size: 20 })

      expect(http.get).toHaveBeenCalledWith('/admin/comments', {
        params: { search: 'hello', page: 0, size: 20 },
      })
      expect(result).toEqual(mockPage)
    })

    it('calls GET /admin/comments without optional params when omitted', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { commentsApi } = await import('@/api/comments')
      await commentsApi.listAll({})

      expect(http.get).toHaveBeenCalledWith('/admin/comments', { params: {} })
    })
  })
})
