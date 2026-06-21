import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ContentView from '@/views/admin/ContentView.vue'
import type { PostSummaryResponse, Page } from '@/api/types'

vi.mock('@/api/posts', () => ({
  postsApi: {
    listAdmin: vi.fn(),
    changeStatus: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('@/api/categories', () => ({
  categoriesApi: {
    list: vi.fn().mockResolvedValue([]),
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

const mockPublishedPost: PostSummaryResponse = {
  id: 1,
  slug: 'published-post',
  title: 'Published Post',
  excerpt: 'A live post',
  status: 'PUBLISHED',
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: '2025-01-02T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: ['vue', 'frontend'],
  viewCount: 500,
  likeCount: 20,
}

const mockDraftPost: PostSummaryResponse = {
  id: 2,
  slug: 'draft-post',
  title: 'Draft Post',
  excerpt: 'Not yet published',
  status: 'DRAFT',
  category: null,
  author: { id: 2, username: 'bob', displayName: 'Bob' },
  publishedAt: null,
  createdAt: '2025-01-05T00:00:00Z',
  tags: [],
  viewCount: 0,
  likeCount: 0,
}

const makePage = (content: PostSummaryResponse[]): Page<PostSummaryResponse> => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
})

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/posts', component: ContentView },
      { path: '/posts/:slug', component: { template: '<div>Post</div>' } },
    ],
  })
}

async function mountContentView() {
  const router = createTestRouter()
  await router.push('/admin/posts')
  const wrapper = mount(ContentView, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('ContentView (admin)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page heading', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost, mockDraftPost]),
    )

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('Content management')
  })

  it('renders the admin.content eyebrow label', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('// admin.content')
  })

  it('displays all posts from the API', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost, mockDraftPost]),
    )

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('Published Post')
    expect(wrapper.text()).toContain('Draft Post')
  })

  it('shows empty state when no posts', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('No posts found')
  })

  it('shows loading spinner before data arrives', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockReturnValue(new Promise(() => {}))

    const router = createTestRouter()
    await router.push('/admin/posts')
    const wrapper = mount(ContentView, {
      global: { plugins: [router] },
    })
    // nextTick processes onMounted which sets loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Loading posts')
  })

  it('shows error message on fetch failure', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Server error'))

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('unexpected error')
  })

  it('debounces search and calls listAdmin with search param', async () => {
    vi.useFakeTimers()
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost]),
    )

    const { wrapper } = await mountContentView()

    const searchInput = wrapper.find('input[type="search"]')
    await searchInput.setValue('vue')

    expect(postsApi.listAdmin).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(400)
    await flushPromises()

    expect(postsApi.listAdmin).toHaveBeenCalledTimes(2)
    expect(postsApi.listAdmin).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: 'vue', page: 0 }),
    )

    vi.useRealTimers()
  })

  it('calls listAdmin with status filter when status dropdown changes', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockDraftPost]),
    )

    const { wrapper } = await mountContentView()

    const statusSelect = wrapper.find('select[aria-label="Filter by status"]')
    await statusSelect.setValue('DRAFT')
    await flushPromises()

    expect(postsApi.listAdmin).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'DRAFT', page: 0 }),
    )
  })

  it('calls postsApi.changeStatus to unpublish a published post', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost]),
    )
    ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockPublishedPost,
      status: 'DRAFT',
    })

    const { wrapper } = await mountContentView()

    const unpublishBtn = wrapper.findAll('button').find((b) => b.text() === 'Unpublish')
    expect(unpublishBtn).toBeTruthy()
    await unpublishBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.changeStatus).toHaveBeenCalledWith(1, { status: 'DRAFT' })
  })

  it('calls postsApi.changeStatus to publish a draft post', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockDraftPost]),
    )
    ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockDraftPost,
      status: 'PUBLISHED',
    })

    const { wrapper } = await mountContentView()

    const publishBtn = wrapper.findAll('button').find((b) => b.text() === 'Publish')
    expect(publishBtn).toBeTruthy()
    await publishBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.changeStatus).toHaveBeenCalledWith(2, { status: 'PUBLISHED' })
  })

  it('calls postsApi.remove when delete is confirmed', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost, mockDraftPost]),
    )
    ;(postsApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountContentView()

    const deleteBtns = wrapper.findAll('button').filter((b) => b.text() === 'Delete')
    expect(deleteBtns.length).toBeGreaterThan(0)
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(postsApi.remove).toHaveBeenCalledWith(1)
    expect(wrapper.text()).not.toContain('Published Post')
    // Draft post stays
    expect(wrapper.text()).toContain('Draft Post')
  })

  it('does NOT call postsApi.remove when delete is cancelled', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost]),
    )

    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const { wrapper } = await mountContentView()

    const deleteBtns = wrapper.findAll('button').filter((b) => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(postsApi.remove).not.toHaveBeenCalled()
  })

  it('renders StatusBadge for each post', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost, mockDraftPost]),
    )

    const { wrapper } = await mountContentView()
    // StatusBadge renders 'published' and 'draft' text
    expect(wrapper.text()).toContain('published')
    expect(wrapper.text()).toContain('draft')
  })

  it('shows author usernames', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listAdmin as ReturnType<typeof vi.fn>).mockResolvedValue(
      makePage([mockPublishedPost, mockDraftPost]),
    )

    const { wrapper } = await mountContentView()
    expect(wrapper.text()).toContain('@alice')
    expect(wrapper.text()).toContain('@bob')
  })
})
