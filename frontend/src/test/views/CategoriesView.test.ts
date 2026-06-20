import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CategoriesView from '@/views/admin/CategoriesView.vue'

vi.mock('@/api/categories', () => ({
  categoriesApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
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

const mockCategories = [
  { id: 1, name: 'Vue', slug: 'vue', description: 'Vue.js framework' },
  { id: 2, name: 'Java', slug: 'java', description: null },
]

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin/categories', component: CategoriesView },
    ],
  })
}

async function mountCategoriesView() {
  const router = createTestRouter()
  await router.push('/admin/categories')
  const wrapper = mount(CategoriesView, {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('CategoriesView (admin)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders page heading', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)

    const { wrapper } = await mountCategoriesView()
    expect(wrapper.text()).toContain('Categories')
  })

  it('displays list of categories from API', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)

    const { wrapper } = await mountCategoriesView()

    expect(wrapper.text()).toContain('Vue')
    expect(wrapper.text()).toContain('Java')
  })

  it('shows empty state when no categories', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue([])

    const { wrapper } = await mountCategoriesView()

    expect(wrapper.text()).toContain('no categories')
  })

  it('opens create panel when Add category is clicked', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue([])

    const { wrapper } = await mountCategoriesView()

    const addBtn = wrapper.findAll('button').find(b => b.text() === 'Add category')
    await addBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Add category')
    // Panel input should be visible
    expect(wrapper.find('#cat-name').exists()).toBe(true)
  })

  it('calls categoriesApi.create with form data when Create is submitted', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue([])
    const newCat = { id: 3, name: 'Spring', slug: 'spring', description: null }
    ;(categoriesApi.create as ReturnType<typeof vi.fn>).mockResolvedValue(newCat)

    const { wrapper } = await mountCategoriesView()

    // Open create panel
    const addBtn = wrapper.findAll('button').find(b => b.text() === 'Add category')
    await addBtn!.trigger('click')

    // Fill form
    await wrapper.find('#cat-name').setValue('Spring')

    // Submit
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(categoriesApi.create).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Spring' }),
    )
  })

  it('shows validation error when name is empty on submit', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue([])

    const { wrapper } = await mountCategoriesView()

    const addBtn = wrapper.findAll('button').find(b => b.text() === 'Add category')
    await addBtn!.trigger('click')

    // Submit without filling name
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(categoriesApi.create).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('required')
  })

  it('opens edit panel with pre-filled data when Edit is clicked', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)

    const { wrapper } = await mountCategoriesView()

    const editBtns = wrapper.findAll('button').filter(b => b.text() === 'Edit')
    expect(editBtns.length).toBeGreaterThan(0)
    await editBtns[0].trigger('click')

    expect(wrapper.text()).toContain('Edit category')
    const nameInput = wrapper.find('#cat-name')
    expect((nameInput.element as HTMLInputElement).value).toBe('Vue')
  })

  it('calls categoriesApi.update when editing and saving', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)
    const updatedCat = { id: 1, name: 'Vue.js', slug: 'vue', description: null }
    ;(categoriesApi.update as ReturnType<typeof vi.fn>).mockResolvedValue(updatedCat)

    const { wrapper } = await mountCategoriesView()

    const editBtns = wrapper.findAll('button').filter(b => b.text() === 'Edit')
    await editBtns[0].trigger('click')

    await wrapper.find('#cat-name').setValue('Vue.js')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(categoriesApi.update).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ name: 'Vue.js' }),
    )
  })

  it('calls categoriesApi.remove when Delete is confirmed', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)
    ;(categoriesApi.remove as ReturnType<typeof vi.fn>).mockResolvedValue(undefined)

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountCategoriesView()

    const deleteBtns = wrapper.findAll('button').filter(b => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(categoriesApi.remove).toHaveBeenCalledWith(1)
    // Row removed from the table (the success toast still echoes the name).
    expect(wrapper.find('table').text()).not.toContain('Vue')
  })

  it('does NOT call categoriesApi.remove when Delete is cancelled', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)

    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const { wrapper } = await mountCategoriesView()

    const deleteBtns = wrapper.findAll('button').filter(b => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    expect(categoriesApi.remove).not.toHaveBeenCalled()
  })

  it('shows 409 conflict error when deleting a referenced category', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue(mockCategories)

    const conflictError = {
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          message: 'Category is referenced by posts',
          status: 409,
          error: 'Conflict',
          timestamp: '',
        },
      },
    }
    ;(categoriesApi.remove as ReturnType<typeof vi.fn>).mockRejectedValue(conflictError)

    // Make axios.isAxiosError return true
    const axios = await import('axios')
    vi.spyOn(axios.default, 'isAxiosError').mockReturnValue(true)

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper } = await mountCategoriesView()

    const deleteBtns = wrapper.findAll('button').filter(b => b.text() === 'Delete')
    await deleteBtns[0].trigger('click')
    await flushPromises()

    // Should show conflict error
    expect(wrapper.text()).toContain('Cannot delete')
  })

  it('closes panel when Cancel is clicked', async () => {
    const { categoriesApi } = await import('@/api/categories')
    ;(categoriesApi.list as ReturnType<typeof vi.fn>).mockResolvedValue([])

    const { wrapper } = await mountCategoriesView()

    const addBtn = wrapper.findAll('button').find(b => b.text() === 'Add category')
    await addBtn!.trigger('click')

    expect(wrapper.find('#cat-name').exists()).toBe(true)

    const cancelBtn = wrapper.findAll('button').find(b => b.text() === 'Cancel')
    await cancelBtn!.trigger('click')

    expect(wrapper.find('#cat-name').exists()).toBe(false)
  })
})
