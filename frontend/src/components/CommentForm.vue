<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useFormValidation, rules } from '@/composables/useFormValidation'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import FormField from '@/components/FormField.vue'

const MAX_LENGTH = 2000

const emit = defineEmits<{
  submitted: [content: string]
}>()

const auth = useAuthStore()

const fields = reactive({ content: '' })
const { errors, validate, clearErrors } = useFormValidation(fields)
const submitting = ref(false)
const submitError = ref<string | null>(null)

const charCount = computed(() => fields.content.length)
const charCountColor = computed(() => {
  if (charCount.value > MAX_LENGTH) return '#F85149'
  if (charCount.value > MAX_LENGTH * 0.8) return '#E6A817'
  return '#8B949E'
})

async function submit() {
  clearErrors()
  submitError.value = null

  const valid = validate({
    content: [
      rules.required('Comment'),
      rules.maxLength(MAX_LENGTH, 'Comment'),
    ],
  })
  if (!valid) return

  submitting.value = true
  try {
    emit('submitted', fields.content.trim())
    fields.content = ''
  } catch {
    submitError.value = 'Failed to post comment. Please try again.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="auth.isAuthenticated">
    <AlertMessage
      v-if="submitError"
      type="error"
      :message="submitError"
      class="mb-3"
    />
    <form @submit.prevent="submit">
      <FormField id="comment-content" label="Add a comment" :error="errors.content">
        <div class="relative">
          <textarea
            id="comment-content"
            v-model="fields.content"
            rows="4"
            placeholder="// share your thoughts..."
            class="w-full px-3 py-2 rounded text-sm resize-y transition-all"
            style="
              background-color: #1C2128;
              color: #E6EDF3;
              border: 1px solid #30363D;
              outline: none;
              font-family: Inter, sans-serif;
              min-height: 90px;
            "
            :maxlength="MAX_LENGTH + 1"
            :aria-describedby="errors.content ? 'comment-content-error' : undefined"
            @focus="($event.target as HTMLTextAreaElement).style.borderColor = '#E6A817'"
            @blur="($event.target as HTMLTextAreaElement).style.borderColor = '#30363D'"
          />
          <span
            class="absolute bottom-2 right-3 text-xs font-mono pointer-events-none"
            :style="{ color: charCountColor, fontFamily: '\'JetBrains Mono\', monospace' }"
            aria-live="polite"
            :aria-label="`${charCount} of ${MAX_LENGTH} characters used`"
          >
            {{ charCount }}/{{ MAX_LENGTH }}
          </span>
        </div>
      </FormField>

      <div class="flex justify-end mt-3">
        <AppButton type="submit" variant="primary" :loading="submitting" :disabled="submitting">
          Post comment
        </AppButton>
      </div>
    </form>
  </div>

  <div v-else class="py-4 text-sm" style="color: #8B949E;">
    <RouterLink to="/login" class="underline" style="color: #E6A817;">Sign in</RouterLink>
    to join the discussion.
  </div>
</template>
