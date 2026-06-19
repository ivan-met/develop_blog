import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'

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
    updateMe: vi.fn(),
    changePassword: vi.fn(),
    listUsers: vi.fn(),
    getUser: vi.fn(),
    updateRoles: vi.fn(),
    updateStatus: vi.fn(),
  },
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/login', component: LoginView },
      { path: '/register', component: { template: '<div>Register</div>' } },
      { path: '/profile', component: { template: '<div>Profile</div>' } },
    ],
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function mountLoginView() {
    const router = createTestRouter()
    await router.push('/login')
    return mount(LoginView, {
      global: {
        plugins: [router],
      },
    })
  }

  it('renders the sign-in heading', async () => {
    const wrapper = await mountLoginView()
    expect(wrapper.find('h1').text()).toBe('Sign in')
  })

  it('renders username/email and password fields', async () => {
    const wrapper = await mountLoginView()
    expect(wrapper.find('#usernameOrEmail').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
  })

  it('shows validation errors when submitting empty form', async () => {
    const wrapper = await mountLoginView()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('required')
  })

  it('shows error for empty username/email field', async () => {
    const wrapper = await mountLoginView()

    await wrapper.find('#password').setValue('somepassword')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Username or email is required')
  })

  it('shows error for empty password field', async () => {
    const wrapper = await mountLoginView()

    await wrapper.find('#usernameOrEmail').setValue('testuser')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Password is required')
  })

  it('calls auth store login with form values on submit', async () => {
    const { authApi } = await import('@/api/auth')
    ;(authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, username: 'test', email: 'test@test.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
    })

    const wrapper = await mountLoginView()
    await wrapper.find('#usernameOrEmail').setValue('testuser')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({
      usernameOrEmail: 'testuser',
      password: 'password123',
    })
  })

  it('displays server error message on API failure', async () => {
    const { authApi } = await import('@/api/auth')
    const axiosError = {
      isAxiosError: true,
      response: {
        status: 401,
        data: { message: 'Invalid credentials', status: 401, error: 'Unauthorized', timestamp: '' },
      },
    }
    ;(authApi.login as ReturnType<typeof vi.fn>).mockRejectedValue(axiosError)

    // Make axios.isAxiosError return true for our mock
    const axios = await import('axios')
    vi.spyOn(axios.default, 'isAxiosError').mockReturnValue(true)

    const wrapper = await mountLoginView()
    await wrapper.find('#usernameOrEmail').setValue('testuser')
    await wrapper.find('#password').setValue('wrong')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Invalid credentials')
  })

  it('has a link to the register page', async () => {
    const wrapper = await mountLoginView()
    const link = wrapper.find('a[href="/register"]')
    expect(link.exists()).toBe(true)
  })
})
