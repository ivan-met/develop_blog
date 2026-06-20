<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const props = defineProps<{
  content: string
}>()

const md: MarkdownIt = new MarkdownIt({
  // Raw HTML is parsed so embeds render, then DOMPurify (below) is the
  // security boundary that strips dangerous tags/attributes/URLs.
  html: true,
  linkify: true,
  typographer: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs-pre"><code class="hljs">' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // fall through to default
      }
    }
    return (
      '<pre class="hljs-pre"><code class="hljs">' +
      md.utils.escapeHtml(str) +
      '</code></pre>'
    )
  },
})

const sanitized = computed(() => {
  const raw = md.render(props.content || '')
  return DOMPurify.sanitize(raw, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'form'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur'],
  })
})
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <article
    class="markdown-body"
    v-html="sanitized"
  />
</template>

<style scoped>
.markdown-body {
  color: #E6EDF3;
  font-family: Inter, sans-serif;
  font-size: 15px;
  line-height: 1.75;
  word-break: break-word;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-family: 'JetBrains Mono', monospace;
  color: #E6EDF3;
  margin-top: 1.75em;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.3;
}

.markdown-body :deep(h1) { font-size: 1.75rem; }
.markdown-body :deep(h2) { font-size: 1.4rem; border-bottom: 1px solid #30363D; padding-bottom: 0.3em; }
.markdown-body :deep(h3) { font-size: 1.15rem; }

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 1em;
}

.markdown-body :deep(a) {
  color: #E6A817;
  text-decoration: underline;
}

.markdown-body :deep(a:hover) {
  color: #B8851A;
}

.markdown-body :deep(code) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85em;
  background-color: rgba(230, 168, 23, 0.1);
  color: #E6A817;
  padding: 0.15em 0.4em;
  border-radius: 3px;
}

.markdown-body :deep(.hljs-pre) {
  background-color: #1C2128;
  border-left: 3px solid #E6A817;
  border-radius: 0 3px 3px 0;
  padding: 1em 1.25em;
  overflow-x: auto;
  margin: 1.25em 0;
}

.markdown-body :deep(.hljs-pre code) {
  background: none;
  color: #E6EDF3;
  padding: 0;
  font-size: 0.875em;
  border-radius: 0;
}

.markdown-body :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 3px solid #30363D;
  color: #8B949E;
  font-style: italic;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.5em;
  margin-bottom: 1em;
}

.markdown-body :deep(li) {
  margin-bottom: 0.25em;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #30363D;
  margin: 2em 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 1em;
  font-size: 0.9em;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #30363D;
  padding: 0.5em 0.75em;
  text-align: left;
}

.markdown-body :deep(th) {
  background-color: #161B22;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8em;
  color: #8B949E;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 3px;
  border: 1px solid #30363D;
}
</style>
