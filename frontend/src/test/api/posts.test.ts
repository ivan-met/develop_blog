import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the http module
vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

// Mock auth and users so auth store initialises without network
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

const mockPost = {
  id: 1,
  slug: 'hello-world',
  title: 'Hello World',
  excerpt: 'Intro',
  status: 'PUBLISHED' as const,
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: '2025-01-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  contentMarkdown: '# Hello',
  updatedAt: '2025-01-01T00:00:00Z',
  tags: ['vue', 'frontend'],
  viewCount: 42,
  likeCount: 0,
  liked: false,
  bookmarked: false,
}

const mockPage = {
  content: [mockPost],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 12,
  first: true,
  last: true,
}

describe('postsApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('listPublished', () => {
    it('calls GET /posts with params and returns page', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.listPublished({ category: 'vue', page: 0, size: 12 })

      expect(http.get).toHaveBeenCalledWith('/posts', {
        params: { category: 'vue', page: 0, size: 12 },
      })
      expect(result).toEqual(mockPage)
    })

    it('forwards sort=popular to GET /posts', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listPublished({ sort: 'popular', page: 0, size: 12 })

      expect(http.get).toHaveBeenCalledWith('/posts', {
        params: { sort: 'popular', page: 0, size: 12 },
      })
    })

    it('forwards sort=latest to GET /posts', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listPublished({ sort: 'latest', page: 0, size: 12 })

      expect(http.get).toHaveBeenCalledWith('/posts', {
        params: { sort: 'latest', page: 0, size: 12 },
      })
    })

    it('forwards category and search together with sort', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listPublished({
        category: 'spring',
        search: 'jwt',
        sort: 'popular',
        page: 0,
        size: 12,
      })

      expect(http.get).toHaveBeenCalledWith('/posts', {
        params: { category: 'spring', search: 'jwt', sort: 'popular', page: 0, size: 12 },
      })
    })
  })

  describe('getBySlug', () => {
    it('calls GET /posts/:slug and returns post', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPost })

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.getBySlug('hello-world')

      expect(http.get).toHaveBeenCalledWith('/posts/hello-world')
      expect(result).toEqual(mockPost)
    })
  })

  describe('listMine', () => {
    it('calls GET /posts/mine with status filter', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listMine({ status: 'DRAFT', page: 0, size: 20 })

      expect(http.get).toHaveBeenCalledWith('/posts/mine', {
        params: { status: 'DRAFT', page: 0, size: 20 },
      })
    })

    it('calls GET /posts/mine without status when undefined', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listMine({ page: 0 })

      expect(http.get).toHaveBeenCalledWith('/posts/mine', {
        params: { page: 0 },
      })
    })
  })

  describe('getMine', () => {
    it('calls GET /posts/mine/:id', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPost })

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.getMine(1)

      expect(http.get).toHaveBeenCalledWith('/posts/mine/1')
      expect(result).toEqual(mockPost)
    })
  })

  describe('create', () => {
    it('calls POST /posts with payload and returns created post', async () => {
      const http = (await import('@/api/http')).default
      ;(http.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPost })

      const { postsApi } = await import('@/api/posts')
      const payload = {
        title: 'Hello World',
        contentMarkdown: '# Hello',
        categoryId: 1,
        status: 'DRAFT' as const,
      }
      const result = await postsApi.create(payload)

      expect(http.post).toHaveBeenCalledWith('/posts', payload)
      expect(result).toEqual(mockPost)
    })
  })

  describe('update', () => {
    it('calls PUT /posts/:id with payload', async () => {
      const http = (await import('@/api/http')).default
      ;(http.put as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPost })

      const { postsApi } = await import('@/api/posts')
      const payload = {
        title: 'Updated Title',
        contentMarkdown: '# Updated',
      }
      const result = await postsApi.update(1, payload)

      expect(http.put).toHaveBeenCalledWith('/posts/1', payload)
      expect(result).toEqual(mockPost)
    })
  })

  describe('changeStatus', () => {
    it('calls PUT /posts/:id/status with status payload', async () => {
      const http = (await import('@/api/http')).default
      ;(http.put as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPost })

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.changeStatus(1, { status: 'PUBLISHED' })

      expect(http.put).toHaveBeenCalledWith('/posts/1/status', { status: 'PUBLISHED' })
      expect(result).toEqual(mockPost)
    })
  })

  describe('remove', () => {
    it('calls DELETE /posts/:id and returns void', async () => {
      const http = (await import('@/api/http')).default
      ;(http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({})

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.remove(1)

      expect(http.delete).toHaveBeenCalledWith('/posts/1')
      expect(result).toBeUndefined()
    })
  })

  describe('listAdmin', () => {
    it('calls GET /admin/posts with all filter params', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      const result = await postsApi.listAdmin({
        status: 'DRAFT',
        search: 'vue',
        categorySlug: 'frontend',
        page: 0,
        size: 20,
      })

      expect(http.get).toHaveBeenCalledWith('/admin/posts', {
        params: { status: 'DRAFT', search: 'vue', categorySlug: 'frontend', page: 0, size: 20 },
      })
      expect(result).toEqual(mockPage)
    })

    it('calls GET /admin/posts without optional params when omitted', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listAdmin({ page: 0 })

      expect(http.get).toHaveBeenCalledWith('/admin/posts', {
        params: { page: 0 },
      })
    })

    it('calls GET /admin/posts with PUBLISHED status filter', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockPage })

      const { postsApi } = await import('@/api/posts')
      await postsApi.listAdmin({ status: 'PUBLISHED', page: 0, size: 20 })

      expect(http.get).toHaveBeenCalledWith('/admin/posts', {
        params: { status: 'PUBLISHED', page: 0, size: 20 },
      })
    })
  })
})
