import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'

vi.mock('@/api/authors', () => ({
  authorsApi: {
    getProfile: vi.fn(),
    getPosts: vi.fn(),
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
  bio: 'Senior engineer building distributed systems.',
  avatarUrl: null,
  createdAt: '2024-03-01T00:00:00Z',
  postCount: 5,
}

const mockPost = {
  id: 1,
  slug: 'intro-to-spring',
  title: 'Intro to Spring',
  excerpt: 'A primer on Spring Boot.',
  status: 'PUBLISHED' as const,
  category: { id: 1, name: 'Java', slug: 'java', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice Dev' },
  publishedAt: '2025-01-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: ['spring', 'java'],
  viewCount: 42,
  likeCount: 8,
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

function createTestRouter(username = 'alice') {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/authors/:username', component: { template: '<div />' } },
      { path: '/posts/:slug', component: { template: '<div />' } },
    ],
  })
}

async function mountView(username = 'alice') {
  const router = createTestRouter(username)
  await router.push(`/authors/${username}`)

  const AuthorProfileView = (await import('@/views/AuthorProfileView.vue')).default

  const wrapper = mount(AuthorProfileView, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        PostCard: {
          template: '<div class="post-card-stub">{{ post.title }}</div>',
          props: ['post'],
        },
        AppButton: {
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
          props: ['variant', 'disabled'],
        },
        AlertMessage: {
          template: '<div class="alert">{{ message }}</div>',
          props: ['type', 'message'],
        },
      },
    },
  })
  await flushPromises()
  return wrapper
}

describe('AuthorProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the author display name', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('Alice Dev')
  })

  it('renders the author username', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('@alice')
  })

  it('renders the author bio', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('Senior engineer building distributed systems.')
  })

  it('renders the post count stat', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('posts')
  })

  it('renders the posts grid with post titles', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    const wrapper = await mountView()
    const cards = wrapper.findAll('.post-card-stub')
    expect(cards).toHaveLength(1)
    expect(cards[0].text()).toContain('Intro to Spring')
  })

  it('shows 404 state when profile not found', async () => {
    const { authorsApi } = await import('@/api/authors')
    const axiosMod = await import('axios')
    // Build a proper axios-like error so isAxiosError returns true
    const axiosError = new axiosMod.AxiosError(
      'Not Found',
      '404',
      undefined,
      undefined,
      { status: 404, data: {}, headers: {}, config: {} as never, statusText: 'Not Found' },
    )
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockRejectedValue(axiosError)

    const wrapper = await mountView('nonexistent')
    expect(wrapper.text()).toContain('Author not found')
  })

  it('shows empty posts state when author has no posts', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 12,
      first: true,
      last: true,
    })

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('no posts yet')
  })

  it('calls getProfile with the route username param', async () => {
    const { authorsApi } = await import('@/api/authors')
    ;(authorsApi.getProfile as ReturnType<typeof vi.fn>).mockResolvedValue(mockProfile)
    ;(authorsApi.getPosts as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage)

    await mountView('alice')
    expect(authorsApi.getProfile).toHaveBeenCalledWith('alice')
  })
})
