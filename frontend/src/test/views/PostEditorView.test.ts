import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PostEditorView from '@/views/PostEditorView.vue'

// Mock API modules
vi.mock('@/api/posts', () => ({
  postsApi: {
    getMine: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    changeStatus: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('@/api/categories', () => ({
  categoriesApi: {
    list: vi.fn().mockResolvedValue([
      { id: 1, name: 'Vue', slug: 'vue', description: null },
      { id: 2, name: 'Java', slug: 'java', description: null },
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

const mockPost = {
  id: 5,
  slug: 'existing-post',
  title: 'Existing Post',
  excerpt: 'Some excerpt',
  status: 'DRAFT' as const,
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice' },
  publishedAt: null,
  createdAt: '2025-01-01T00:00:00Z',
  contentMarkdown: '# Existing content',
  updatedAt: '2025-01-01T00:00:00Z',
  tags: ['vue', 'typescript'],
  viewCount: 0,
  likeCount: 0,
  liked: false,
  bookmarked: false,
}

function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/posts/new',
        component: PostEditorView,
        meta: { requiresAuth: true },
      },
      {
        path: '/posts/:id/edit',
        component: PostEditorView,
        meta: { requiresAuth: true },
      },
      {
        path: '/me/posts',
        component: { template: '<div>My Posts</div>' },
      },
    ],
  })
  return router
}

async function mountEditor(path = '/posts/new') {
  const router = createTestRouter()
  await router.push(path)
  const wrapper = mount(PostEditorView, {
    global: {
      plugins: [router],
      stubs: {
        MarkdownEditor: {
          template: '<div class="markdown-editor-stub"><textarea id="contentMarkdown" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" /></div>',
          props: ['modelValue', 'disabled'],
          emits: ['update:modelValue'],
        },
        CategorySelect: {
          template: '<select id="categoryId" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value ? Number($event.target.value) : null)"><option value="">Select</option><option value="1">Vue</option></select>',
          props: ['modelValue', 'hasError', 'disabled'],
          emits: ['update:modelValue'],
        },
      },
    },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('PostEditorView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('new post mode', () => {
    it('renders the new post heading', async () => {
      const { wrapper } = await mountEditor('/posts/new')
      expect(wrapper.text()).toContain('New post')
    })

    it('shows validation error when title is empty and Save draft is clicked', async () => {
      const { wrapper } = await mountEditor('/posts/new')
      // Click Save draft without filling form
      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      expect(saveDraftBtn).toBeTruthy()
      await saveDraftBtn!.trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('required')
    })

    it('calls postsApi.create with DRAFT status when Save draft is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      const createdPost = { ...mockPost, id: 10, title: 'My New Post', status: 'DRAFT' as const }
      ;(postsApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(createdPost)

      const { wrapper, router } = await mountEditor('/posts/new')

      // Fill in title
      await wrapper.find('#title').setValue('My New Post')
      // Fill in content via the stubbed textarea
      await wrapper.find('#contentMarkdown').setValue('# My content')

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'My New Post',
          contentMarkdown: '# My content',
          status: 'DRAFT',
        }),
      )
      // Should redirect to edit view
      expect(router.currentRoute.value.path).toBe('/posts/10/edit')
    })

    it('calls postsApi.create with PUBLISHED status and categoryId when Publish is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      const createdPost = { ...mockPost, id: 11, status: 'PUBLISHED' as const }
      ;(postsApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(createdPost)

      const { wrapper, router } = await mountEditor('/posts/new')

      await wrapper.find('#title').setValue('My Published Post')
      await wrapper.find('#contentMarkdown').setValue('# Content')
      // Select category
      await wrapper.find('#categoryId').setValue('1')

      const publishBtn = wrapper.findAll('button').find(b => b.text() === 'Publish')
      await publishBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'My Published Post',
          contentMarkdown: '# Content',
          categoryId: 1,
          status: 'PUBLISHED',
        }),
      )
      expect(router.currentRoute.value.path).toBe('/posts/11/edit')
    })

    it('shows error when publishing without a category', async () => {
      const { wrapper } = await mountEditor('/posts/new')

      await wrapper.find('#title').setValue('Post Without Category')
      await wrapper.find('#contentMarkdown').setValue('# Some content')
      // Do NOT select a category

      const publishBtn = wrapper.findAll('button').find(b => b.text() === 'Publish')
      await publishBtn!.trigger('click')
      await flushPromises()

      const { postsApi } = await import('@/api/posts')
      // Should not call create
      expect(postsApi.create).not.toHaveBeenCalled()
      // Should show category error
      expect(wrapper.text()).toContain('required to publish')
    })
  })

  describe('edit post mode', () => {
    it('loads existing post and shows edit heading', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      expect(postsApi.getMine).toHaveBeenCalledWith(5)
      expect(wrapper.text()).toContain('Edit post')
    })

    it('pre-fills form with existing post data', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      const titleInput = wrapper.find('#title')
      expect((titleInput.element as HTMLInputElement).value).toBe('Existing Post')
    })

    it('calls postsApi.update when saving draft on existing post', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)
      ;(postsApi.update as ReturnType<typeof vi.fn>).mockResolvedValue({ ...mockPost, title: 'Updated Title' })

      const { wrapper } = await mountEditor('/posts/5/edit')

      await wrapper.find('#title').setValue('Updated Title')
      await wrapper.find('#contentMarkdown').setValue('# Updated content')

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.update).toHaveBeenCalledWith(
        5,
        expect.objectContaining({
          title: 'Updated Title',
          contentMarkdown: '# Updated content',
        }),
      )
    })

    it('calls postsApi.update then changeStatus when publishing existing post', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)
      ;(postsApi.update as ReturnType<typeof vi.fn>).mockResolvedValue({ ...mockPost })
      ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({ ...mockPost, status: 'PUBLISHED' as const })

      const { wrapper } = await mountEditor('/posts/5/edit')

      await wrapper.find('#title').setValue('Ready to Publish')
      await wrapper.find('#contentMarkdown').setValue('# Content')
      await wrapper.find('#categoryId').setValue('1')

      const publishBtn = wrapper.findAll('button').find(b => b.text() === 'Publish')
      await publishBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.update).toHaveBeenCalledWith(
        5,
        expect.objectContaining({ title: 'Ready to Publish' }),
      )
      expect(postsApi.changeStatus).toHaveBeenCalledWith(5, { status: 'PUBLISHED' })
    })

    it('shows Unpublish button when post is PUBLISHED', async () => {
      const { postsApi } = await import('@/api/posts')
      const publishedPost = { ...mockPost, status: 'PUBLISHED' as const }
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(publishedPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      const unpublishBtn = wrapper.findAll('button').find(b => b.text() === 'Unpublish')
      expect(unpublishBtn).toBeTruthy()
    })

    it('does not show Unpublish button when post is DRAFT', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      const unpublishBtn = wrapper.findAll('button').find(b => b.text() === 'Unpublish')
      expect(unpublishBtn).toBeUndefined()
    })

    it('calls postsApi.changeStatus with DRAFT when Unpublish is clicked', async () => {
      const { postsApi } = await import('@/api/posts')
      const publishedPost = { ...mockPost, status: 'PUBLISHED' as const }
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(publishedPost)
      ;(postsApi.changeStatus as ReturnType<typeof vi.fn>).mockResolvedValue({ ...mockPost, status: 'DRAFT' as const })

      const { wrapper } = await mountEditor('/posts/5/edit')

      const unpublishBtn = wrapper.findAll('button').find(b => b.text() === 'Unpublish')
      await unpublishBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.changeStatus).toHaveBeenCalledWith(5, { status: 'DRAFT' })
    })
  })

  describe('tags input', () => {
    it('renders a tags input field', async () => {
      const { wrapper } = await mountEditor('/posts/new')
      const tagsInput = wrapper.find('#tagsInput')
      expect(tagsInput.exists()).toBe(true)
    })

    it('serializes comma-separated tags to string[] in create payload', async () => {
      const { postsApi } = await import('@/api/posts')
      const createdPost = { ...mockPost, id: 20, status: 'DRAFT' as const }
      ;(postsApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(createdPost)

      const { wrapper } = await mountEditor('/posts/new')

      await wrapper.find('#title').setValue('Tagged Post')
      await wrapper.find('#contentMarkdown').setValue('# Content')
      await wrapper.find('#tagsInput').setValue('vue, typescript, testing')

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          tags: ['vue', 'typescript', 'testing'],
        }),
      )
    })

    it('trims and lowercases tags in create payload', async () => {
      const { postsApi } = await import('@/api/posts')
      const createdPost = { ...mockPost, id: 21, status: 'DRAFT' as const }
      ;(postsApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(createdPost)

      const { wrapper } = await mountEditor('/posts/new')

      await wrapper.find('#title').setValue('Tagged Post')
      await wrapper.find('#contentMarkdown').setValue('# Content')
      await wrapper.find('#tagsInput').setValue('  Vue  ,  TypeScript ,REST')

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          tags: ['vue', 'typescript', 'rest'],
        }),
      )
    })

    it('sends empty tags array when tags input is blank', async () => {
      const { postsApi } = await import('@/api/posts')
      const createdPost = { ...mockPost, id: 22, status: 'DRAFT' as const }
      ;(postsApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(createdPost)

      const { wrapper } = await mountEditor('/posts/new')

      await wrapper.find('#title').setValue('No Tags Post')
      await wrapper.find('#contentMarkdown').setValue('# Content')
      // Leave tagsInput empty

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          tags: [],
        }),
      )
    })

    it('pre-fills tags from existing post on edit', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      const tagsInput = wrapper.find('#tagsInput')
      expect((tagsInput.element as HTMLInputElement).value).toBe('vue, typescript')
    })

    it('serializes tags to string[] in update payload', async () => {
      const { postsApi } = await import('@/api/posts')
      ;(postsApi.getMine as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)
      ;(postsApi.update as ReturnType<typeof vi.fn>).mockResolvedValue(mockPost)

      const { wrapper } = await mountEditor('/posts/5/edit')

      await wrapper.find('#tagsInput').setValue('spring, security')

      const saveDraftBtn = wrapper.findAll('button').find(b => b.text().includes('Save draft'))
      await saveDraftBtn!.trigger('click')
      await flushPromises()

      expect(postsApi.update).toHaveBeenCalledWith(
        5,
        expect.objectContaining({
          tags: ['spring', 'security'],
        }),
      )
    })
  })
})
