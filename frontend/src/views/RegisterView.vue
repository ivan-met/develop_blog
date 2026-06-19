<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const serverError = ref<string | null>(null)
const { errors, validate, setServerErrors } = useFormValidation(form)

async function handleSubmit() {
  serverError.value = null

  const valid = validate({
    username: [
      rules.required('Username'),
      rules.noSpaces(),
      rules.minLength(3, 'Username'),
      rules.maxLength(50, 'Username'),
    ],
    email: [rules.required('Email'), rules.email()],
    password: [
      rules.required('Password'),
      rules.minLength(8, 'Password'),
    ],
    confirmPassword: [
      rules.required('Confirm password'),
      rules.matches(form.password, 'Passwords'),
    ],
  })

  if (!valid) return

  try {
    await auth.register({
      username: form.username,
      email: form.email,
      password: form.password,
    })
    await router.push('/profile')
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) {
        setServerErrors(data.fieldErrors)
      } else {
        serverError.value =
          data?.message ?? 'Registration failed. Please try again.'
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
        >// create_account</p>

        <h1
          class="text-2xl font-mono font-semibold mb-1"
          style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
        >
          Create account
        </h1>
        <p class="text-sm mb-8" style="color: #8B949E;">
          Join the platform. Start sharing technical posts.
        </p>

        <AlertMessage
          v-if="serverError"
          type="error"
          :message="serverError"
          class="mb-6"
        />

        <form novalidate class="flex flex-col gap-5" @submit.prevent="handleSubmit">
          <FormField
            id="username"
            label="Username"
            :error="errors.username"
            hint="3–50 characters, no spaces"
          >
            <AppInput
              id="username"
              v-model="form.username"
              type="text"
              placeholder="your_handle"
              autocomplete="username"
              :has-error="!!errors.username"
              :disabled="auth.loading"
              :aria-describedby="errors.username ? 'username-error' : undefined"
            />
          </FormField>

          <FormField
            id="email"
            label="Email"
            :error="errors.email"
          >
            <AppInput
              id="email"
              v-model="form.email"
              type="email"
              placeholder="you@example.com"
              autocomplete="email"
              :has-error="!!errors.email"
              :disabled="auth.loading"
              :aria-describedby="errors.email ? 'email-error' : undefined"
            />
          </FormField>

          <FormField
            id="password"
            label="Password"
            :error="errors.password"
            hint="Minimum 8 characters"
          >
            <AppInput
              id="password"
              v-model="form.password"
              type="password"
              placeholder="Choose a strong password"
              autocomplete="new-password"
              :has-error="!!errors.password"
              :disabled="auth.loading"
              :aria-describedby="errors.password ? 'password-error' : undefined"
            />
          </FormField>

          <FormField
            id="confirmPassword"
            label="Confirm password"
            :error="errors.confirmPassword"
          >
            <AppInput
              id="confirmPassword"
              v-model="form.confirmPassword"
              type="password"
              placeholder="Repeat your password"
              autocomplete="new-password"
              :has-error="!!errors.confirmPassword"
              :disabled="auth.loading"
              :aria-describedby="errors.confirmPassword ? 'confirmPassword-error' : undefined"
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
            Create account
          </AppButton>
        </form>

        <p class="mt-6 text-sm text-center" style="color: #8B949E;">
          Already have an account?
          <RouterLink
            to="/login"
            class="font-medium no-underline transition-colors"
            style="color: #E6A817;"
          >
            Sign in
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
