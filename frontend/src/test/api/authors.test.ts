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

const mockProfile = {
  username: 'alice',
  displayName: 'Alice Dev',
  bio: 'Software engineer.',
  avatarUrl: null,
  createdAt: '2024-01-01T00:00:00Z',
  postCount: 7,
}

const mockPostSummary = {
  id: 1,
  slug: 'hello-world',
  title: 'Hello World',
  excerpt: 'Intro',
  status: 'PUBLISHED' as const,
  category: null,
  author: { id: 1, username: 'alice', displayName: 'Alice Dev' },
  publishedAt: '2025-01-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: [],
  viewCount: 10,
  likeCount: 3,
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

describe('authorsApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('getProfile', () => {
    it('calls GET /authors/:username and returns AuthorProfileResponse', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockProfile })

      const { authorsApi } = await import('@/api/authors')
      const result = await authorsApi.getProfile('alice')

      expect(http.get).toHaveBeenCalledWith('/authors/alice')
      expect(result).toEqual(mockProfile)
    })
  })

  describe('getPosts', () => {
    it('calls GET /authors/:username/posts with page and size params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { authorsApi } = await import('@/api/authors')
      const result = await authorsApi.getPosts('alice', { page: 0, size: 12 })

      expect(http.get).toHaveBeenCalledWith('/authors/alice/posts', {
        params: { page: 0, size: 12 },
      })
      expect(result).toEqual(mockPage)
    })

    it('works without optional params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { authorsApi } = await import('@/api/authors')
      await authorsApi.getPosts('alice', {})

      expect(http.get).toHaveBeenCalledWith('/authors/alice/posts', {
        params: {},
      })
    })
  })
})
