import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'

// Mock auth store
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

async function mountCommentForm(isAuthenticated = true) {
  const { useAuthStore } = await import('@/stores/auth')
  ;(useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated,
  })

  const router = createTestRouter()
  await router.push('/')
  const CommentForm = (await import('@/components/CommentForm.vue')).default

  return mount(CommentForm, {
    global: {
      plugins: [router],
      stubs: {
        AppButton: {
          template: '<button type="submit" :disabled="loading || disabled" @click="$emit(\'click\')"><slot /></button>',
          props: ['type', 'variant', 'loading', 'disabled'],
        },
        AlertMessage: {
          template: '<div class="alert">{{ message }}</div>',
          props: ['type', 'message'],
        },
        FormField: {
          template: '<div><slot /></div>',
          props: ['id', 'label', 'error'],
        },
      },
    },
  })
}

describe('CommentForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the textarea when authenticated', async () => {
    const wrapper = await mountCommentForm(true)
    expect(wrapper.find('textarea').exists()).toBe(true)
  })

  it('shows sign-in prompt when unauthenticated', async () => {
    const wrapper = await mountCommentForm(false)
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.text()).toContain('Sign in')
  })

  it('emits submitted event with content on valid form submit', async () => {
    const wrapper = await mountCommentForm(true)
    const textarea = wrapper.find('textarea')
    await textarea.setValue('This is a great post!')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const emitted = wrapper.emitted('submitted')
    expect(emitted).toBeTruthy()
    expect(emitted![0]).toEqual(['This is a great post!'])
  })

  it('does not emit when content is empty', async () => {
    const wrapper = await mountCommentForm(true)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.emitted('submitted')).toBeFalsy()
  })

  it('clears the textarea after successful submission', async () => {
    const wrapper = await mountCommentForm(true)
    const textarea = wrapper.find('textarea')
    await textarea.setValue('My comment here')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect((textarea.element as HTMLTextAreaElement).value).toBe('')
  })

  it('shows character count', async () => {
    const wrapper = await mountCommentForm(true)
    const textarea = wrapper.find('textarea')
    await textarea.setValue('Hello')
    // Char counter shows current/max
    expect(wrapper.text()).toContain('5/2000')
  })
})
