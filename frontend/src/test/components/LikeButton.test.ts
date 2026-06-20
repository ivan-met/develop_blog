import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia, defineStore } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'

// Mock engagement API
vi.mock('@/api/engagement', () => ({
  engagementApi: {
    like: vi.fn(),
    unlike: vi.fn(),
    bookmark: vi.fn(),
    removeBookmark: vi.fn(),
    listMyBookmarks: vi.fn(),
  },
}))

// Mock auth store: we control isAuthenticated
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', component: { template: '<div />' } },
    ],
  })
}

async function mountLikeButton(
  props: { slug: string; liked: boolean; likeCount: number },
  isAuthenticated = true,
) {
  const { useAuthStore } = await import('@/stores/auth')
  ;(useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated,
  })

  const router = createTestRouter()
  await router.push('/')
  const LikeButton = (await import('@/components/LikeButton.vue')).default

  return mount(LikeButton, {
    props,
    global: { plugins: [router] },
  })
}

describe('LikeButton', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the like count', async () => {
    const wrapper = await mountLikeButton({ slug: 'my-post', liked: false, likeCount: 7 })
    expect(wrapper.text()).toContain('7')
  })

  it('shows heart outline when not liked', async () => {
    const wrapper = await mountLikeButton({ slug: 'my-post', liked: false, likeCount: 0 })
    const svg = wrapper.find('svg')
    expect(svg.attributes('fill')).toBe('none')
  })

  it('shows filled heart when liked', async () => {
    const wrapper = await mountLikeButton({ slug: 'my-post', liked: true, likeCount: 3 })
    const svg = wrapper.find('svg')
    expect(svg.attributes('fill')).toBe('currentColor')
  })

  it('applies optimistic update on click (not liked → liked)', async () => {
    const { engagementApi } = await import('@/api/engagement')
    // Delay resolution to inspect optimistic state
    let resolve!: (v: unknown) => void
    ;(engagementApi.like as ReturnType<typeof vi.fn>).mockReturnValue(
      new Promise((r) => { resolve = r }),
    )

    const wrapper = await mountLikeButton({ slug: 'my-post', liked: false, likeCount: 5 })
    await wrapper.find('button').trigger('click')

    // Optimistic: count should be 6 and heart filled before promise resolves
    expect(wrapper.text()).toContain('6')
    expect(wrapper.find('svg').attributes('fill')).toBe('currentColor')

    // Resolve the promise
    resolve({ likeCount: 6, liked: true })
    await flushPromises()
  })

  it('reverts optimistic update on API error', async () => {
    const { engagementApi } = await import('@/api/engagement')
    ;(engagementApi.like as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Network error'))

    const wrapper = await mountLikeButton({ slug: 'my-post', liked: false, likeCount: 5 })
    await wrapper.find('button').trigger('click')
    await flushPromises()

    // Should revert to original values
    expect(wrapper.text()).toContain('5')
    expect(wrapper.find('svg').attributes('fill')).toBe('none')
  })

  it('emits change event with updated values on success', async () => {
    const { engagementApi } = await import('@/api/engagement')
    ;(engagementApi.like as ReturnType<typeof vi.fn>).mockResolvedValue({ likeCount: 8, liked: true })

    const wrapper = await mountLikeButton({ slug: 'my-post', liked: false, likeCount: 7 })
    await wrapper.find('button').trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('change')
    expect(emitted).toBeTruthy()
    expect(emitted![0]).toEqual([true, 8])
  })

  it('redirects to /login when unauthenticated', async () => {
    const wrapper = await mountLikeButton(
      { slug: 'my-post', liked: false, likeCount: 0 },
      false, // not authenticated
    )
    const router = wrapper.vm.$router
    const push = vi.spyOn(router, 'push')

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/login')
  })

  it('calls unlike API when already liked', async () => {
    const { engagementApi } = await import('@/api/engagement')
    ;(engagementApi.unlike as ReturnType<typeof vi.fn>).mockResolvedValue({ likeCount: 2, liked: false })

    const wrapper = await mountLikeButton({ slug: 'my-post', liked: true, likeCount: 3 })
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(engagementApi.unlike).toHaveBeenCalledWith('my-post')
  })
})
