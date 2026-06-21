import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CommentsView from '@/views/admin/CommentsView.vue'
import type { AdminCommentResponse, Page } from '@/api/types'

vi.mock('@/api/comments', () => ({
  commentsApi: {
    listAll: vi.fn(),
    remove: vi.fn(),
    list: vi.fn(),
    create: vi.fn(),
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

const mockComments: AdminCommentResponse[] = [
  {
    id: 1,
    content: 'This is a great article about Vue!',
    author: { id: 2, username: 'alice', displayName: 'Alice' },
    postSlug: 'intro-to-vue',
    postTitle: 'Introduction to Vue',
    createdAt: '2025-01-10T00:00:00Z',
  },
  {
    id: 2,
    content: 'Thanks for sharing this knowledge.',
    author: { id: 3, username: 'bob', displayName: 'Bob' },
    postSlug: 'spring-boot-basics',
    postTitle: 'Spring Boot Basics',
    createdAt: '2025-01-11T00:00:00Z',
  },
]

const makePage = (content: AdminCommentResponse[]): Page<AdminCommentResponse> => ({
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
      { path: '/admin/comments', component: CommentsView },
      { path: '/posts/:slug', component: { template: '<div>Post</div>' } },
    ],
  })
}

async function mountCommentsView() {
  const router = createTestRouter()
  await router.push('/admin/comments')
  const wrapper = mount(CommentsView, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('CommentsView (admin)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page heading', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    const { wrapper } = await mountCommentsView()
    expect(wrapper.text()).toContain('Comment moderation')
  })

  it('renders the admin.comments eyebrow label', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    const { wrapper } = await mountCommentsView()
    expect(wrapper.text()).toContain('// admin.comments')
  })

  it('displays comments from the API', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    const { wrapper } = await mountCommentsView()
    expect(wrapper.text()).toContain('@alice')
    expect(wrapper.text()).toContain('@bob')
    expect(wrapper.text()).toContain('This is a great article about Vue!')
  })

  it('shows empty state when no comments', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

    const { wrapper } = await mountCommentsView()
    expect(wrapper.text()).toContain('No comments found')
  })

  it('shows loading spinner before data arrives', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockReturnValue(new Promise(() => {}))

    const router = createTestRouter()
    await router.push('/admin/comments')
    const wrapper = mount(CommentsView, {
      global: { plugins: [router] },
    })
    // nextTick processes onMounted which sets loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Loading comments')
  })

  it('shows error message on fetch failure', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Server error'))

    const { wrapper } = await mountCommentsView()
    expect(wrapper.text()).toContain('unexpected error')
  })

  it('debounces search and calls listAll with search param', async () => {
    vi.useFakeTimers()
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    const { wrapper } = await mountCommentsView()

    const searchInput = wrapper.find('input[type="search"]')
    await searchInput.setValue('alice')

    // Before debounce fires, should not have called again
    expect(commentsApi.listAll).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(400)
    await flushPromises()

    expect(commentsApi.listAll).toHaveBeenCalledTimes(2)
    expect(commentsApi.listAll).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: 'alice', page: 0 }),
    )

    vi.useRealTimers()
  })

  it('calls commentsApi.remove when delete is confirmed', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))
    ;(commentsApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountCommentsView()

    const deleteBtns = wrapper.findAll('button').filter((b) => b.text() === 'Delete')
    expect(deleteBtns.length).toBeGreaterThan(0)
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(commentsApi.remove).toHaveBeenCalledWith(1)
  })

  it('removes the deleted comment from the list after deletion', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))
    ;(commentsApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountCommentsView()

    const deleteBtns = wrapper.findAll('button').filter((b) => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(wrapper.text()).not.toContain('@alice')
    // bob's comment should still be there
    expect(wrapper.text()).toContain('@bob')
  })

  it('does NOT call commentsApi.remove when delete is cancelled', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const { wrapper } = await mountCommentsView()

    const deleteBtns = wrapper.findAll('button').filter((b) => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(commentsApi.remove).not.toHaveBeenCalled()
  })

  it('shows empty state with search message when search yields no results', async () => {
    const { commentsApi } = await import('@/api/comments')
    // Initial load returns data
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce(makePage(mockComments))
      .mockResolvedValue(makePage([]))

    vi.useFakeTimers()
    const { wrapper } = await mountCommentsView()

    const searchInput = wrapper.find('input[type="search"]')
    await searchInput.setValue('nonexistent')

    vi.advanceTimersByTime(400)
    await flushPromises()

    expect(wrapper.text()).toContain('No comments match your search')
    vi.useRealTimers()
  })

  it('renders post title as a link', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.listAll as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(mockComments))

    const { wrapper } = await mountCommentsView()

    const links = wrapper.findAll('a')
    const postLink = links.find((l) => l.text().includes('Introduction to Vue'))
    expect(postLink).toBeDefined()
  })
})
