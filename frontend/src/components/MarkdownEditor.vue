<script setup lang="ts">
import { ref } from 'vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'

const props = defineProps<{
  modelValue: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// Mobile tab: 'write' | 'preview'
const mobileTab = ref<'write' | 'preview'>('write')

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div class="markdown-editor">
    <!-- Mobile tab switcher (hidden on sm+) -->
    <div
      class="flex sm:hidden border-b"
      style="border-color: #30363D; flex-shrink: 0;"
    >
      <button
        type="button"
        class="flex-1 py-2 text-xs font-mono font-medium transition-colors cursor-pointer"
        :style="{
          background: 'transparent',
          border: 'none',
          color: mobileTab === 'write' ? '#E6A817' : '#8B949E',
          borderBottom: mobileTab === 'write' ? '2px solid #E6A817' : '2px solid transparent',
        }"
        @click="mobileTab = 'write'"
      >
        Write
      </button>
      <button
        type="button"
        class="flex-1 py-2 text-xs font-mono font-medium transition-colors cursor-pointer"
        :style="{
          background: 'transparent',
          border: 'none',
          color: mobileTab === 'preview' ? '#E6A817' : '#8B949E',
          borderBottom: mobileTab === 'preview' ? '2px solid #E6A817' : '2px solid transparent',
        }"
        @click="mobileTab = 'preview'"
      >
        Preview
      </button>
    </div>

    <!-- Panes container -->
    <div class="editor-panes">
      <!-- Write pane: always visible on desktop; visible on mobile when tab = write -->
      <div class="editor-write-pane" :class="{ 'pane-hidden-mobile': mobileTab !== 'write' }">
        <div
          class="hidden sm:flex items-center px-3 py-1.5 border-b flex-shrink-0"
          style="background-color: #161B22; border-color: #30363D;"
        >
          <span
            class="text-xs font-mono"
            style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
          >// markdown</span>
        </div>
        <textarea
          :value="props.modelValue"
          :disabled="disabled"
          class="editor-textarea"
          placeholder="Write your post in Markdown…"
          aria-label="Post content (Markdown)"
          spellcheck="true"
          @input="onInput"
          @focus="($event.target as HTMLTextAreaElement).style.borderLeftColor = '#E6A817'"
          @blur="($event.target as HTMLTextAreaElement).style.borderLeftColor = '#30363D'"
        />
      </div>

      <!-- Preview pane: always visible on desktop; visible on mobile when tab = preview -->
      <div
        class="editor-preview-pane"
        :class="{ 'pane-hidden-mobile': mobileTab !== 'preview' }"
      >
        <div
          class="hidden sm:flex items-center px-3 py-1.5 border-b flex-shrink-0"
          style="background-color: #1C2128; border-color: #30363D;"
        >
          <span
            class="text-xs font-mono"
            style="color: #8B949E; font-family: 'JetBrains Mono', monospace;"
          >// preview</span>
        </div>
        <div class="preview-scroll">
          <MarkdownPreview :content="props.modelValue" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.markdown-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #30363D;
  border-radius: 3px;
  overflow: hidden;
}

.editor-panes {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* Write pane */
.editor-write-pane {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
}

/* Preview pane */
.editor-preview-pane {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
  background-color: #1C2128;
}

/* On desktop (sm+): separator between panes */
@media (min-width: 640px) {
  .editor-write-pane {
    border-right: 1px solid #30363D;
  }
}

/* On mobile: hide non-active pane using display:none */
@media (max-width: 639px) {
  .pane-hidden-mobile {
    display: none;
  }
}

.editor-textarea {
  flex: 1;
  width: 100%;
  padding: 1rem;
  resize: none;
  background-color: #161B22;
  color: #E6EDF3;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.875rem;
  line-height: 1.7;
  border: none;
  border-left: 3px solid #30363D;
  outline: none;
  transition: border-left-color 0.15s;
  min-height: 280px;
}

.editor-textarea::placeholder {
  color: #8B949E;
}

.editor-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.preview-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}
</style>
