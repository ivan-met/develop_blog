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
  usersApi: {
    getMe: vi.fn().mockResolvedValue(null),
  },
}))

const mockCategory = {
  id: 1,
  name: 'Vue',
  slug: 'vue',
  description: 'Everything Vue.js',
}

describe('categoriesApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('list', () => {
    it('calls GET /categories and returns array', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [mockCategory] })

      const { categoriesApi } = await import('@/api/categories')
      const result = await categoriesApi.list()

      expect(http.get).toHaveBeenCalledWith('/categories')
      expect(result).toEqual([mockCategory])
    })
  })

  describe('getBySlug', () => {
    it('calls GET /categories/:slug', async () => {
      const http = (await import('@/api/http')).default
      ;(http.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockCategory })

      const { categoriesApi } = await import('@/api/categories')
      const result = await categoriesApi.getBySlug('vue')

      expect(http.get).toHaveBeenCalledWith('/categories/vue')
      expect(result).toEqual(mockCategory)
    })
  })

  describe('create (admin)', () => {
    it('calls POST /admin/categories with payload', async () => {
      const http = (await import('@/api/http')).default
      ;(http.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockCategory })

      const { categoriesApi } = await import('@/api/categories')
      const payload = { name: 'Vue', description: 'Everything Vue.js' }
      const result = await categoriesApi.create(payload)

      expect(http.post).toHaveBeenCalledWith('/admin/categories', payload)
      expect(result).toEqual(mockCategory)
    })
  })

  describe('update (admin)', () => {
    it('calls PUT /admin/categories/:id with payload', async () => {
      const http = (await import('@/api/http')).default
      ;(http.put as ReturnType<typeof vi.fn>).mockResolvedValue({ data: mockCategory })

      const { categoriesApi } = await import('@/api/categories')
      const payload = { name: 'Vue.js', description: 'Updated description' }
      const result = await categoriesApi.update(1, payload)

      expect(http.put).toHaveBeenCalledWith('/admin/categories/1', payload)
      expect(result).toEqual(mockCategory)
    })
  })

  describe('remove (admin)', () => {
    it('calls DELETE /admin/categories/:id and returns void', async () => {
      const http = (await import('@/api/http')).default
      ;(http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({})

      const { categoriesApi } = await import('@/api/categories')
      const result = await categoriesApi.remove(1)

      expect(http.delete).toHaveBeenCalledWith('/admin/categories/1')
      expect(result).toBeUndefined()
    })
  })
})
