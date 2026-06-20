import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownPreview from '@/components/MarkdownPreview.vue'

describe('MarkdownPreview', () => {
  it('renders plain markdown text', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '**Hello** World' },
    })
    expect(wrapper.html()).toContain('Hello')
    expect(wrapper.html()).toContain('<strong>')
  })

  it('renders headings', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '# My Heading' },
    })
    expect(wrapper.html()).toContain('<h1>')
    expect(wrapper.html()).toContain('My Heading')
  })

  it('sanitizes <script> tags — XSS injection is stripped', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: 'Hello <script>alert("xss")</script> World' },
    })
    expect(wrapper.html()).not.toContain('<script>')
    expect(wrapper.html()).not.toContain('alert')
  })

  it('sanitizes onerror attribute — XSS via img onerror is stripped', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '<img src="x" onerror="alert(1)">' },
    })
    expect(wrapper.html()).not.toContain('onerror')
    // The img element itself may or may not be present, but onerror must not be
  })

  it('sanitizes javascript: href in anchor tags', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '<a href="javascript:alert(1)">click me</a>' },
    })
    // DOMPurify strips javascript: hrefs from raw HTML anchors
    expect(wrapper.html()).not.toContain('javascript:')
  })

  it('sanitizes onload attribute on a tag', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '<a onload="alert(1)">link</a>' },
    })
    expect(wrapper.html()).not.toContain('onload')
  })

  it('sanitizes onclick attribute', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '<div onclick="evil()">click</div>' },
    })
    expect(wrapper.html()).not.toContain('onclick')
  })

  it('allows safe HTML that comes through markdown-it (links, code)', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: 'Visit [Vue](https://vuejs.org) and use `const x = 1`' },
    })
    expect(wrapper.html()).toContain('https://vuejs.org')
    expect(wrapper.html()).toContain('<code>')
  })

  it('renders code blocks with hljs-pre class', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '```javascript\nconst x = 1\n```' },
    })
    expect(wrapper.html()).toContain('hljs-pre')
  })

  it('handles empty content gracefully', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '' },
    })
    // Should not throw, should render an empty article
    expect(wrapper.find('article').exists()).toBe(true)
  })

  it('sanitizes style injection via iframe', () => {
    const wrapper = mount(MarkdownPreview, {
      props: { content: '<iframe src="https://evil.com"></iframe>' },
    })
    expect(wrapper.html()).not.toContain('<iframe')
  })
})
