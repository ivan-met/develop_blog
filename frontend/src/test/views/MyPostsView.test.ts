import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import MyPostsView from '@/views/MyPostsView.vue'
import type { PostSummaryResponse } from '@/api/types'

vi.mock('@/api/posts', () => ({
  postsApi: {
    listMine: vi.fn(),
    changeStatus: vi.fn(),
    remove: vi.fn(),
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

const mockDraftPost = {
  id: 1,
  slug: 'draft-post',
  title: 'Draft Post',
  excerpt: 'A draft',
  status: 'DRAFT' as const,
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: null,
  createdAt: '2025-01-01T00:00:00Z',
  tags: [] as string[],
  viewCount: 0,
  likeCount: 0,
}

const mockPublishedPost = {
  id: 2,
  slug: 'published-post',
  title: 'Published Post',
  excerpt: 'Live!',
  status: 'PUBLISHED' as const,
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: '2025-01-02T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: [] as string[],
  viewCount: 5,
  likeCount: 0,
}

const mockPage = (content: PostSummaryResponse[]) => ({
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
      { path: '/me/posts', component: MyPostsView },
      { path: '/posts/:id/edit', component: { template: '<div>Edit</div>' } },
      { path: '/posts/new', component: { template: '<div>New</div>' } },
    ],
  })
}

async function mountMyPosts() {
  const router = createTestRouter()
  await router.push('/me/posts')
  const wrapper = mount(MyPostsView, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('MyPostsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders page heading', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage([]))

    const { wrapper } = await mountMyPosts()
    expect(wrapper.text()).toContain('My posts')
  })

  it('shows posts returned from listMine', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockDraftPost, mockPublishedPost]),
    )

    const { wrapper } = await mountMyPosts()

    expect(wrapper.text()).toContain('Draft Post')
    expect(wrapper.text()).toContain('Published Post')
  })

  it('shows empty state when no posts', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage([]))

    const { wrapper } = await mountMyPosts()

    expect(wrapper.text()).toContain('no posts yet')
  })

  it('calls listMine with DRAFT filter when Drafts tab is clicked', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage([mockDraftPost]))

    const { wrapper } = await mountMyPosts()

    const draftsTab = wrapper.findAll('button').find(b => b.text() === 'Drafts')
    expect(draftsTab).toBeTruthy()
    await draftsTab!.trigger('click')
    await flushPromises()

    expect(postsApi.listMine).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'DRAFT', page: 0 }),
    )
  })

  it('calls listMine with PUBLISHED filter when Published tab is clicked', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPage([mockPublishedPost]))

    const { wrapper } = await mountMyPosts()

    const publishedTab = wrapper.findAll('button').find(b => b.text() === 'Published')
    await publishedTab!.trigger('click')
    await flushPromises()

    expect(postsApi.listMine).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'PUBLISHED', page: 0 }),
    )
  })

  it('calls postsApi.changeStatus to publish a DRAFT post', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockDraftPost]),
    )
    ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockDraftPost,
      status: 'PUBLISHED',
    })

    const { wrapper } = await mountMyPosts()

    // The Publish button for the draft post
    const publishBtn = wrapper.findAll('button').find(b => b.text() === 'Publish')
    expect(publishBtn).toBeTruthy()
    await publishBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.changeStatus).toHaveBeenCalledWith(1, { status: 'PUBLISHED' })
  })

  it('calls postsApi.changeStatus to unpublish a PUBLISHED post', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockPublishedPost]),
    )
    ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockPublishedPost,
      status: 'DRAFT',
    })

    const { wrapper } = await mountMyPosts()

    const unpublishBtn = wrapper.findAll('button').find(b => b.text() === 'Unpublish')
    expect(unpublishBtn).toBeTruthy()
    await unpublishBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.changeStatus).toHaveBeenCalledWith(2, { status: 'DRAFT' })
  })

  it('calls postsApi.remove when Delete is confirmed', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockDraftPost]),
    )
    ;(postsApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

    // Mock window.confirm to return true
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountMyPosts()

    const deleteBtn = wrapper.findAll('button').find(b => b.text() === 'Delete')
    expect(deleteBtn).toBeTruthy()
    await deleteBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.remove).toHaveBeenCalledWith(1)
    // Post should be removed from the list
    expect(wrapper.text()).not.toContain('Draft Post')
  })

  it('does NOT call postsApi.remove when Delete is cancelled', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockDraftPost]),
    )

    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const { wrapper } = await mountMyPosts()

    const deleteBtn = wrapper.findAll('button').find(b => b.text() === 'Delete')
    await deleteBtn!.trigger('click')
    await flushPromises()

    expect(postsApi.remove).not.toHaveBeenCalled()
  })

  it('navigates to /posts/:id/edit when Edit is clicked', async () => {
    const { postsApi } = await import('@/api/posts')
    ;(postsApi.listMine as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockPage([mockDraftPost]),
    )

    const { wrapper, router } = await mountMyPosts()

    const editBtn = wrapper.findAll('button').find(b => b.text() === 'Edit')
    expect(editBtn).toBeTruthy()
    await editBtn!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/posts/1/edit')
  })
})
