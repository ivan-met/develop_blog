import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PostsListView from '@/views/PostsListView.vue'
import type { PostSummaryResponse } from '@/api/types'

vi.mock('@/api/posts', () => ({
  postsApi: {
    listPublished: vi.fn(),
  },
}))

vi.mock('@/api/categories', () => ({
  categoriesApi: {
    list: vi.fn().mockResolvedValue([
      { id: 1, name: 'Java', slug: 'java', description: null },
      { id: 2, name: 'Vue', slug: 'vue', description: null },
    ]),
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

const makePost = (overrides: Partial<PostSummaryResponse> = {}): PostSummaryResponse => ({
  id: 1,
  slug: 'hello-world',
  title: 'Hello World',
  excerpt: 'An intro.',
  status: 'PUBLISHED',
  category: { id: 1, name: 'Java', slug: 'java', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: '2025-01-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  tags: ['spring', 'jwt'],
  viewCount: 10,
  ...overrides,
})

const makePage = (posts: PostSummaryResponse[]) => ({
  content: posts,
  totalElements: posts.length,
  totalPages: 1,
  number: 0,
  size: 12,
  first: true,
  last: true,
})

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: PostsListView },
      { path: '/posts/:slug', component: { template: '<div />' } },
    ],
  })
}

async function mountView() {
  const router = createTestRouter()
  await router.push('/')
  const wrapper = mount(PostsListView, {
    global: {
      plugins: [router],
      stubs: {
        // Stub PostCard so we can test view logic without card internals
        PostCard: {
          template: `<div class="post-card-stub">
            <span>{{ post.title }}</span>
            <button
              v-for="tag in post.tags"
              :key="tag"
              class="tag-btn"
              @click="$emit('tagClick', tag)"
            >{{ tag }}</button>
          </div>`,
          props: ['post', 'showStatus'],
          emits: ['tagClick'],
        },
      },
    },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('PostsListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('sort toggle', () => {
    it('renders Latest and Most Popular toggle buttons', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      expect(wrapper.text()).toContain('Latest')
      expect(wrapper.text()).toContain('Most Popular')
    })

    it('defaults to sort=latest on initial fetch', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      await mountView()

      expect(postsApi.listPublished).toHaveBeenCalledWith(
        expect.objectContaining({ sort: 'latest' }),
      )
    })

    it('refetches with sort=popular when Most Popular is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      const popularBtn = wrapper.findAll('button').find(b => b.text() === 'Most Popular')
      expect(popularBtn).toBeTruthy()
      await popularBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ sort: 'popular', page: 0 }),
      )
    })

    it('refetches with sort=latest when Latest is clicked after switching to popular', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      const popularBtn = wrapper.findAll('button').find(b => b.text() === 'Most Popular')
      await popularBtn!.trigger('click')
      await flushPromises()

      const latestBtn = wrapper.findAll('button').find(b => b.text() === 'Latest')
      await latestBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ sort: 'latest', page: 0 }),
      )
    })

    it('resets to page 0 when sort mode changes', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      const popularBtn = wrapper.findAll('button').find(b => b.text() === 'Most Popular')
      await popularBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 0 }),
      )
    })
  })

  describe('tag chip click', () => {
    it('sets the search input to the clicked tag', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(
        makePage([makePost({ tags: ['spring', 'jwt'] })]),
      )

      const { wrapper } = await mountView()

      // Click the 'spring' tag chip emitted from the stubbed PostCard
      const tagBtns = wrapper.findAll('.tag-btn')
      const springBtn = tagBtns.find(b => b.text() === 'spring')
      expect(springBtn).toBeTruthy()
      await springBtn!.trigger('click')
      await flushPromises()

      // The search input should now contain 'spring'
      const searchInput = wrapper.find('input[type="search"]')
      expect((searchInput.element as HTMLInputElement).value).toBe('spring')
    })

    it('refetches with search=tag after clicking a tag chip', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(
        makePage([makePost({ tags: ['jwt'] })]),
      )

      const { wrapper } = await mountView()

      vi.useFakeTimers()

      const tagBtns = wrapper.findAll('.tag-btn')
      const jwtBtn = tagBtns.find(b => b.text() === 'jwt')
      await jwtBtn!.trigger('click')

      // Advance debounce timer
      vi.advanceTimersByTime(400)
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ search: 'jwt' }),
      )

      vi.useRealTimers()
    })
  })

  describe('category filter', () => {
    it('renders Technology section label when categories are loaded', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      expect(wrapper.text()).toContain('Technology')
    })

    it('refetches with category when a category chip is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      // Find the Java chip (categories are stubbed as Java + Vue)
      const javaBtn = wrapper.findAll('button').find(b => b.text() === 'Java')
      expect(javaBtn).toBeTruthy()
      await javaBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ category: 'java', page: 0 }),
      )
    })

    it('refetches without category when All chip is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      // First select Java
      const javaBtn = wrapper.findAll('button').find(b => b.text() === 'Java')
      await javaBtn!.trigger('click')
      await flushPromises()

      // Then click All
      const allBtn = wrapper.findAll('button').find(b => b.text() === 'All')
      await allBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.listPublished).toHaveBeenLastCalledWith(
        expect.objectContaining({ category: undefined }),
      )
    })
  })

  describe('posts display', () => {
    it('shows post titles from the API response', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(
        makePage([makePost({ title: 'Getting Started with Spring' })]),
      )

      const { wrapper } = await mountView()

      expect(wrapper.text()).toContain('Getting Started with Spring')
    })

    it('shows empty state when no posts are returned', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.listPublished as ReturnType<typeof vi.fn>).mockResolvedValue(makePage([]))

      const { wrapper } = await mountView()

      expect(wrapper.text()).toContain('no posts found')
    })
  })
})
