import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PostCard from '@/components/PostCard.vue'
import type { PostSummaryResponse } from '@/api/types'

// Stub StatusBadge so we don't need to import the full component tree
vi.mock('@/components/StatusBadge.vue', () => ({
  default: { template: '<span class="status-badge-stub">{{ status }}</span>', props: ['status'] },
}))

const basePost: PostSummaryResponse = {
  id: 1,
  slug: 'test-post',
  title: 'Test Post Title',
  excerpt: 'A short excerpt.',
  status: 'PUBLISHED',
  category: { id: 1, name: 'Vue', slug: 'vue', description: null },
  author: { id: 1, username: 'alice', displayName: 'Alice Dev' },
  publishedAt: '2025-03-15T10:00:00Z',
  createdAt: '2025-03-14T00:00:00Z',
  tags: ['vue', 'composition-api', 'typescript'],
  viewCount: 1250,
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/posts/:slug', component: { template: '<div />' } },
    ],
  })
}

async function mountCard(post: PostSummaryResponse = basePost) {
  const router = createTestRouter()
  await router.push('/')
  return mount(PostCard, {
    props: { post },
    global: { plugins: [router] },
  })
}

describe('PostCard', () => {
  describe('viewCount display', () => {
    it('renders the view count when below 1000', async () => {
      const post = { ...basePost, viewCount: 42 }
      const wrapper = await mountCard(post)
      expect(wrapper.text()).toContain('42')
    })

    it('renders view count in k format for values >= 1000', async () => {
      const post = { ...basePost, viewCount: 1250 }
      const wrapper = await mountCard(post)
      expect(wrapper.text()).toContain('1.3k')
    })

    it('renders view count of exactly 1000 as "1k"', async () => {
      const post = { ...basePost, viewCount: 1000 }
      const wrapper = await mountCard(post)
      expect(wrapper.text()).toContain('1k')
    })

    it('renders view count of 0', async () => {
      const post = { ...basePost, viewCount: 0 }
      const wrapper = await mountCard(post)
      expect(wrapper.text()).toContain('0')
    })
  })

  describe('tags display', () => {
    it('renders all tag chips', async () => {
      const wrapper = await mountCard()
      const tagButtons = wrapper.findAll('button[aria-label^="Search for tag"]')
      expect(tagButtons).toHaveLength(3)
      expect(tagButtons[0].text()).toBe('vue')
      expect(tagButtons[1].text()).toBe('composition-api')
      expect(tagButtons[2].text()).toBe('typescript')
    })

    it('does not render tag section when tags is empty', async () => {
      const post = { ...basePost, tags: [] }
      const wrapper = await mountCard(post)
      const tagButtons = wrapper.findAll('button[aria-label^="Search for tag"]')
      expect(tagButtons).toHaveLength(0)
    })

    it('emits tagClick with the correct tag when a tag chip is clicked', async () => {
      const wrapper = await mountCard()
      const tagButtons = wrapper.findAll('button[aria-label^="Search for tag"]')
      await tagButtons[1].trigger('click')
      const emitted = wrapper.emitted('tagClick')
      expect(emitted).toBeTruthy()
      expect(emitted![0]).toEqual(['composition-api'])
    })

    it('emits tagClick for the first tag', async () => {
      const wrapper = await mountCard()
      const tagButtons = wrapper.findAll('button[aria-label^="Search for tag"]')
      await tagButtons[0].trigger('click')
      expect(wrapper.emitted('tagClick')![0]).toEqual(['vue'])
    })
  })

  describe('existing card content', () => {
    it('renders the post title', async () => {
      const wrapper = await mountCard()
      expect(wrapper.text()).toContain('Test Post Title')
    })

    it('renders the category chip', async () => {
      const wrapper = await mountCard()
      expect(wrapper.text()).toContain('Vue')
    })

    it('renders the author display name', async () => {
      const wrapper = await mountCard()
      expect(wrapper.text()).toContain('Alice Dev')
    })

    it('renders the excerpt', async () => {
      const wrapper = await mountCard()
      expect(wrapper.text()).toContain('A short excerpt.')
    })

    it('links to the post slug', async () => {
      const wrapper = await mountCard()
      const link = wrapper.find('a')
      expect(link.attributes('href')).toBe('/posts/test-post')
    })
  })
})
