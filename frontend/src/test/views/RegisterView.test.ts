import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import RegisterView from '@/views/RegisterView.vue'

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
      { path: '/login', component: { template: '<div>Login</div>' } },
      { path: '/register', component: RegisterView },
      { path: '/profile', component: { template: '<div>Profile</div>' } },
    ],
  })
}

describe('RegisterView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function mountRegisterView() {
    const router = createTestRouter()
    await router.push('/register')
    return mount(RegisterView, {
      global: {
        plugins: [router],
      },
    })
  }

  it('renders the create account heading', async () => {
    const wrapper = await mountRegisterView()
    expect(wrapper.find('h1').text()).toBe('Create account')
  })

  it('renders all registration fields', async () => {
    const wrapper = await mountRegisterView()
    expect(wrapper.find('#username').exists()).toBe(true)
    expect(wrapper.find('#email').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })

  it('shows validation errors when submitting empty form', async () => {
    const wrapper = await mountRegisterView()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('required')
  })

  it('shows error when username is too short', async () => {
    const wrapper = await mountRegisterView()

    await wrapper.find('#username').setValue('ab')
    await wrapper.find('#email').setValue('test@test.com')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('#confirmPassword').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('at least 3')
  })

  it('shows error when email is invalid', async () => {
    const wrapper = await mountRegisterView()

    await wrapper.find('#username').setValue('validuser')
    await wrapper.find('#email').setValue('not-an-email')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('#confirmPassword').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('valid email')
  })

  it('shows error when password is too short', async () => {
    const wrapper = await mountRegisterView()

    await wrapper.find('#username').setValue('validuser')
    await wrapper.find('#email').setValue('test@test.com')
    await wrapper.find('#password').setValue('short')
    await wrapper.find('#confirmPassword').setValue('short')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('at least 8')
  })

  it('shows error when passwords do not match', async () => {
    const wrapper = await mountRegisterView()

    await wrapper.find('#username').setValue('validuser')
    await wrapper.find('#email').setValue('test@test.com')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('#confirmPassword').setValue('different456')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('do not match')
  })

  it('calls auth store register with correct values on valid submit', async () => {
    const { authApi } = await import('@/api/auth')
    ;(authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, username: 'newuser', email: 'new@example.com', displayName: null, bio: null, avatarUrl: null, roles: ['USER'], active: true, createdAt: '2024-01-01' },
    })

    const wrapper = await mountRegisterView()
    await wrapper.find('#username').setValue('newuser')
    await wrapper.find('#email').setValue('new@example.com')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('#confirmPassword').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.register).toHaveBeenCalledWith({
      username: 'newuser',
      email: 'new@example.com',
      password: 'password123',
    })
  })

  it('displays server error on API failure', async () => {
    const { authApi } = await import('@/api/auth')
    const axiosError = {
      isAxiosError: true,
      response: {
        status: 409,
        data: { message: 'Username already taken', status: 409, error: 'Conflict', timestamp: '' },
      },
    }
    ;(authApi.register as ReturnType<typeof vi.fn>).mockRejectedValue(axiosError)
    const axios = await import('axios')
    vi.spyOn(axios.default, 'isAxiosError').mockReturnValue(true)

    const wrapper = await mountRegisterView()
    await wrapper.find('#username').setValue('taken')
    await wrapper.find('#email').setValue('x@example.com')
    await wrapper.find('#password').setValue('password123')
    await wrapper.find('#confirmPassword').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Username already taken')
  })

  it('has a link to the login page', async () => {
    const wrapper = await mountRegisterView()
    const link = wrapper.find('a[href="/login"]')
    expect(link.exists()).toBe(true)
  })
})
