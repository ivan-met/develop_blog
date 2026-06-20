import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import type { CommentResponse } from '@/api/types'

vi.mock('@/api/comments', () => ({
  commentsApi: {
    list: vi.fn(),
    create: vi.fn(),
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
  usersApi: { getMe: vi.fn().mockResolvedValue(null) },
}))

function makeComment(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    content: 'Great post!',
    author: { id: 2, username: 'bob', displayName: 'Bob Dev' },
    createdAt: new Date(Date.now() - 5 * 60_000).toISOString(), // 5 min ago
    canDelete: false,
    ...overrides,
  }
}

function makePage(comments: CommentResponse[]) {
  return {
    content: comments,
    totalElements: comments.length,
    totalPages: 1,
    number: 0,
    size: 10,
    first: true,
    last: true,
  }
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: { template: '<div />' } }],
  })
}

async function mountCommentList(comments: CommentResponse[] = [makeComment()]) {
  const { commentsApi } = await import('@/api/comments')
  ;(commentsApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(makePage(comments))

  const router = createTestRouter()
  await router.push('/')
  const CommentList = (await import('@/components/CommentList.vue')).default

  const wrapper = mount(CommentList, {
    props: { slug: 'test-post' },
    global: {
      plugins: [createPinia(), router],
      stubs: {
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

describe('CommentList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders comment content', async () => {
    const wrapper = await mountCommentList([makeComment({ content: 'Fantastic article!' })])
    expect(wrapper.text()).toContain('Fantastic article!')
  })

  it('renders author display name', async () => {
    const wrapper = await mountCommentList([makeComment({ author: { id: 2, username: 'bob', displayName: 'Bob Dev' } })])
    expect(wrapper.text()).toContain('Bob Dev')
  })

  it('shows relative timestamp', async () => {
    const wrapper = await mountCommentList([makeComment()])
    // 5 min ago comment
    expect(wrapper.text()).toMatch(/\d+m ago/)
  })

  it('renders delete button when canDelete is true', async () => {
    const wrapper = await mountCommentList([makeComment({ id: 99, canDelete: true })])
    const deleteBtn = wrapper.find('[aria-label^="Delete comment"]')
    expect(deleteBtn.exists()).toBe(true)
  })

  it('does NOT render delete button when canDelete is false', async () => {
    const wrapper = await mountCommentList([makeComment({ canDelete: false })])
    const deleteBtn = wrapper.find('[aria-label^="Delete comment"]')
    expect(deleteBtn.exists()).toBe(false)
  })

  it('calls commentsApi.remove and re-fetches on delete', async () => {
    const { commentsApi } = await import('@/api/comments')
    ;(commentsApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)
    // After delete, list returns empty
    ;(commentsApi.list as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce(makePage([makeComment({ id: 10, canDelete: true })]))
      .mockResolvedValueOnce(makePage([]))

    const wrapper = await mountCommentList([makeComment({ id: 10, canDelete: true })])

    const deleteBtn = wrapper.find('[aria-label^="Delete comment"]')
    await deleteBtn.trigger('click')
    await flushPromises()

    expect(commentsApi.remove).toHaveBeenCalledWith(10)
    expect(commentsApi.list).toHaveBeenCalledTimes(2)
  })

  it('shows empty state when no comments', async () => {
    const wrapper = await mountCommentList([])
    expect(wrapper.text()).toContain('no comments yet')
  })

  it('exposes prependComment to add a comment without re-fetching', async () => {
    const wrapper = await mountCommentList([])
    const newComment = makeComment({ id: 99, content: 'Prepended!' })

    ;(wrapper.vm as unknown as { prependComment: (c: CommentResponse) => void }).prependComment(newComment)
    await flushPromises()

    expect(wrapper.text()).toContain('Prepended!')
  })
})
