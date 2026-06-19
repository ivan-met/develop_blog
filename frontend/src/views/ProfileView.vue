<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { useFormValidation, rules } from '@/composables/useFormValidation'
import FormField from '@/components/FormField.vue'
import AppInput from '@/components/AppInput.vue'
import AppButton from '@/components/AppButton.vue'
import AlertMessage from '@/components/AlertMessage.vue'
import type { ApiError } from '@/api/types'
import axios from 'axios'

const auth = useAuthStore()

// ─── Profile form ─────────────────────────────────────────────────────────────
const profileForm = reactive({
  displayName: auth.currentUser?.displayName ?? '',
  bio: auth.currentUser?.bio ?? '',
  avatarUrl: auth.currentUser?.avatarUrl ?? '',
  email: auth.currentUser?.email ?? '',
})

// Sync form when user loads
watch(
  () => auth.currentUser,
  (user) => {
    if (user) {
      profileForm.displayName = user.displayName ?? ''
      profileForm.bio = user.bio ?? ''
      profileForm.avatarUrl = user.avatarUrl ?? ''
      profileForm.email = user.email
    }
  },
)

const profileSaving = ref(false)
const profileSuccess = ref<string | null>(null)
const profileError = ref<string | null>(null)
const { errors: profileErrors, validate: validateProfile, setServerErrors: setProfileServerErrors } =
  useFormValidation(profileForm)

async function saveProfile() {
  profileSuccess.value = null
  profileError.value = null

  const valid = validateProfile({
    email: [rules.required('Email'), rules.email()],
  })
  if (!valid) return

  profileSaving.value = true
  try {
    const updated = await usersApi.updateMe({
      displayName: profileForm.displayName || undefined,
      bio: profileForm.bio || undefined,
      avatarUrl: profileForm.avatarUrl || undefined,
      email: profileForm.email,
    })
    auth.currentUser = updated
    profileSuccess.value = 'Profile updated.'
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) setProfileServerErrors(data.fieldErrors)
      else profileError.value = data?.message ?? 'Could not save profile.'
    } else {
      profileError.value = 'An unexpected error occurred.'
    }
  } finally {
    profileSaving.value = false
  }
}

// ─── Password form ────────────────────────────────────────────────────────────
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const passwordSaving = ref(false)
const passwordSuccess = ref<string | null>(null)
const passwordError = ref<string | null>(null)
const { errors: passwordErrors, validate: validatePassword, setServerErrors: setPasswordServerErrors } =
  useFormValidation(passwordForm)

async function changePassword() {
  passwordSuccess.value = null
  passwordError.value = null

  const valid = validatePassword({
    currentPassword: [rules.required('Current password')],
    newPassword: [
      rules.required('New password'),
      rules.minLength(8, 'New password'),
    ],
    confirmNewPassword: [
      rules.required('Confirm password'),
      rules.matches(passwordForm.newPassword, 'Passwords'),
    ],
  })
  if (!valid) return

  passwordSaving.value = true
  try {
    await usersApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmNewPassword = ''
    passwordSuccess.value = 'Password changed.'
  } catch (err: unknown) {
    if (axios.isAxiosError(err)) {
      const data = err.response?.data as ApiError | undefined
      if (data?.fieldErrors) setPasswordServerErrors(data.fieldErrors)
      else passwordError.value = data?.message ?? 'Could not change password.'
    } else {
      passwordError.value = 'An unexpected error occurred.'
    }
  } finally {
    passwordSaving.value = false
  }
}

function roleLabel(role: string): string {
  return role === 'ADMIN' ? 'Admin' : role === 'USER' ? 'User' : role
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-10">
    <!-- Page header -->
    <div class="mb-8">
      <p
        class="text-xs font-mono mb-2"
        style="color: #E6A817; font-family: 'JetBrains Mono', monospace;"
        aria-hidden="true"
      >// your_profile</p>
      <h1
        class="text-2xl font-mono font-semibold"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Profile
      </h1>
      <p class="text-sm mt-1" style="color: #8B949E;">
        Manage your identity on the platform.
      </p>
    </div>

    <!-- Username + roles (read-only info) -->
    <div
      class="rounded-lg p-5 mb-6 flex items-center gap-4"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <div
        class="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0 font-mono font-semibold text-lg"
        style="background-color: #1C2128; color: #E6A817; font-family: 'JetBrains Mono', monospace; border: 1px solid #30363D;"
      >
        {{ auth.currentUser?.username?.charAt(0).toUpperCase() ?? '?' }}
      </div>
      <div class="min-w-0">
        <div class="font-mono font-medium text-sm" style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;">
          {{ auth.currentUser?.username ?? '…' }}
        </div>
        <div class="flex flex-wrap gap-1.5 mt-1.5">
          <span
            v-for="role in auth.currentUser?.roles ?? []"
            :key="role"
            class="text-xs px-2 py-0.5 rounded font-mono"
            :style="{
              backgroundColor: role === 'ADMIN' ? 'rgba(230, 168, 23, 0.15)' : 'rgba(63, 185, 80, 0.15)',
              color: role === 'ADMIN' ? '#E6A817' : '#3FB950',
              border: `1px solid ${role === 'ADMIN' ? 'rgba(230, 168, 23, 0.3)' : 'rgba(63, 185, 80, 0.3)'}`,
              fontFamily: '\'JetBrains Mono\', monospace',
            }"
          >
            {{ roleLabel(role) }}
          </span>
        </div>
      </div>
      <div class="ml-auto text-xs" style="color: #8B949E;">
        Joined {{ auth.currentUser?.createdAt ? new Date(auth.currentUser.createdAt).toLocaleDateString() : '…' }}
      </div>
    </div>

    <!-- Profile edit form -->
    <section
      class="rounded-lg p-6 mb-6"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <h2
        class="text-base font-mono font-semibold mb-5"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Account details
      </h2>

      <AlertMessage
        v-if="profileError"
        type="error"
        :message="profileError"
        class="mb-5"
      />
      <AlertMessage
        v-if="profileSuccess"
        type="success"
        :message="profileSuccess"
        class="mb-5"
      />

      <form novalidate class="flex flex-col gap-5" @submit.prevent="saveProfile">
        <FormField id="email" label="Email" :error="profileErrors.email">
          <AppInput
            id="email"
            v-model="profileForm.email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
            :has-error="!!profileErrors.email"
            :disabled="profileSaving"
          />
        </FormField>

        <FormField id="displayName" label="Display name" :error="profileErrors.displayName">
          <AppInput
            id="displayName"
            v-model="profileForm.displayName"
            type="text"
            placeholder="How you appear to others"
            :has-error="!!profileErrors.displayName"
            :disabled="profileSaving"
          />
        </FormField>

        <FormField id="avatarUrl" label="Avatar URL" :error="profileErrors.avatarUrl">
          <AppInput
            id="avatarUrl"
            v-model="profileForm.avatarUrl"
            type="url"
            placeholder="https://example.com/avatar.jpg"
            :has-error="!!profileErrors.avatarUrl"
            :disabled="profileSaving"
          />
        </FormField>

        <FormField id="bio" label="Bio" :error="profileErrors.bio">
          <textarea
            id="bio"
            v-model="profileForm.bio"
            rows="3"
            placeholder="A short description of yourself and what you write about"
            class="w-full px-3 py-2.5 rounded text-sm resize-y transition-all"
            style="background-color: #161B22; color: #E6EDF3; border: 1px solid #30363D; outline: none; font-family: Inter, sans-serif; min-height: 80px;"
            :disabled="profileSaving"
            @focus="($event.target as HTMLTextAreaElement).style.borderColor = '#E6A817'"
            @blur="($event.target as HTMLTextAreaElement).style.borderColor = '#30363D'"
          />
        </FormField>

        <div class="flex justify-end">
          <AppButton
            type="submit"
            variant="primary"
            :loading="profileSaving"
            :disabled="profileSaving"
          >
            Save changes
          </AppButton>
        </div>
      </form>
    </section>

    <!-- Password change form -->
    <section
      class="rounded-lg p-6"
      style="background-color: #161B22; border: 1px solid #30363D;"
    >
      <h2
        class="text-base font-mono font-semibold mb-5"
        style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
      >
        Change password
      </h2>

      <AlertMessage
        v-if="passwordError"
        type="error"
        :message="passwordError"
        class="mb-5"
      />
      <AlertMessage
        v-if="passwordSuccess"
        type="success"
        :message="passwordSuccess"
        class="mb-5"
      />

      <form novalidate class="flex flex-col gap-5" @submit.prevent="changePassword">
        <FormField
          id="currentPassword"
          label="Current password"
          :error="passwordErrors.currentPassword"
        >
          <AppInput
            id="currentPassword"
            v-model="passwordForm.currentPassword"
            type="password"
            placeholder="Your current password"
            autocomplete="current-password"
            :has-error="!!passwordErrors.currentPassword"
            :disabled="passwordSaving"
          />
        </FormField>

        <FormField
          id="newPassword"
          label="New password"
          :error="passwordErrors.newPassword"
          hint="Minimum 8 characters"
        >
          <AppInput
            id="newPassword"
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="Choose a new password"
            autocomplete="new-password"
            :has-error="!!passwordErrors.newPassword"
            :disabled="passwordSaving"
          />
        </FormField>

        <FormField
          id="confirmNewPassword"
          label="Confirm new password"
          :error="passwordErrors.confirmNewPassword"
        >
          <AppInput
            id="confirmNewPassword"
            v-model="passwordForm.confirmNewPassword"
            type="password"
            placeholder="Repeat the new password"
            autocomplete="new-password"
            :has-error="!!passwordErrors.confirmNewPassword"
            :disabled="passwordSaving"
          />
        </FormField>

        <div class="flex justify-end">
          <AppButton
            type="submit"
            variant="secondary"
            :loading="passwordSaving"
            :disabled="passwordSaving"
          >
            Change password
          </AppButton>
        </div>
      </form>
    </section>
  </div>
</template>
