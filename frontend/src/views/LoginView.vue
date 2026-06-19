<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFormValidation, rules } from '@/composables/useFormValidation'
import FormField from '@/components/FormField.vue'
import AppInput from '@/components/AppInput.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { ApiError } from '@/api/types'
import axios from 'axios'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  usernameOrEmail: '',
  password: '',
})

const serverError = ref<string | null>(null)
const { errors, validate, setServerErrors } = useFormValidation(form)

async function handleSubmit() {
  serverError.value = null

  const valid = validate({
    usernameOrEmail: [rules.required('Username or email')],
    password: [rules.required('Password')],
  })

  if (!valid) return

  try {
    await auth.login({
      usernameOrEmail: form.usernameOrEmail,
      password: form.password,
    })

    const redirect = route.query.redirect as string | undefined
    await router.push(redirect ?? '/profile')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) {
        setServerErrors(data.fieldErrors)
      } else {
        serverError.value =
          data?.message ?? 'Sign in failed. Check your credentials and try again.'
      }
    } else {
      serverError.value = 'An unexpected error occurred.'
    }
  }
}
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-md">
      <!-- Card -->
      <div
        class="rounded-lg p-8"
        style="background-color: #161B22; border: 1px solid #30363D;"
      >
        <!-- Code comment eyebrow -->
        <p
          class="text-xs font-mono mb-6"
          style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          aria-hidden="true"
        >// authenticate</p>

        <h1
          class="text-2xl font-mono font-semibold mb-1"
          style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
        >
          Sign in
        </h1>
        <p class="text-sm mb-8" style="color: #8B949E;">
          Welcome back. Enter your credentials to continue.
        </p>

        <AlertMessage
          v-if="serverError"
          type="error"
          :message="serverError"
          class="mb-6"
        />

        <form novalidate class="flex flex-col gap-5" @submit.prevent="handleSubmit">
          <FormField
            id="usernameOrEmail"
            label="Username or email"
            :error="errors.usernameOrEmail"
          >
            <AppInput
              id="usernameOrEmail"
              v-model="form.usernameOrEmail"
              type="text"
              placeholder="you@example.com"
              autocomplete="username"
              :has-error="!!errors.usernameOrEmail"
              :disabled="auth.loading"
              :aria-describedby="errors.usernameOrEmail ? 'usernameOrEmail-error' : undefined"
            />
          </FormField>

          <FormField
            id="password"
            label="Password"
            :error="errors.password"
          >
            <AppInput
              id="password"
              v-model="form.password"
              type="password"
              placeholder="Your password"
              autocomplete="current-password"
              :has-error="!!errors.password"
              :disabled="auth.loading"
              :aria-describedby="errors.password ? 'password-error' : undefined"
            />
          </FormField>

          <AppButton
            type="submit"
            variant="primary"
            :loading="auth.loading"
            :disabled="auth.loading"
            full-width
            class="mt-2"
          >
            Sign in
          </AppButton>
        </form>

        <p class="mt-6 text-sm text-center" style="color: #8B949E;">
          No account?
          <RouterLink
            to="/register"
            class="font-medium no-underline transition-colors"
            style="color: #E6A817;"
          >
            Create one
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
