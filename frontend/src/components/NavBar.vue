<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const mobileMenuOpen = ref(false)

async function handleLogout() {
  await auth.logout()
  mobileMenuOpen.value = false
  router.push('/login')
}

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}
</script>

<template>
  <header
    class="sticky top-0 z-50 border-b"
    style="background-color: #161B22; border-color: #30363D;"
  >
    <nav class="max-w-7xl mx-auto px-4 sm:px-6" aria-label="Main navigation">
      <div class="flex items-center justify-between h-16">
        <!-- Logo / brand -->
        <RouterLink
          to="/"
          class="flex items-center gap-2 no-underline group"
          @click="closeMobileMenu"
        >
          <span
            class="text-xs font-mono tracking-wider px-2 py-0.5 rounded"
            style="color: #0D1117; background-color: #E6A817; font-family: 'JetBrains Mono', monospace;"
          >&lt;/&gt;</span>
          <span
            class="font-mono font-semibold text-lg tracking-tight"
            style="color: #E6EDF3; font-family: 'JetBrains Mono', monospace;"
          >DevBlog</span>
        </RouterLink>

        <!-- Desktop nav -->
        <div class="hidden sm:flex items-center gap-1">
          <template v-if="auth.isAuthenticated">
            <RouterLink
              v-if="auth.isAdmin"
              to="/admin/users"
              class="nav-link px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
              style="color: #8B949E;"
              active-class="nav-link-active"
            >
              User Admin
            </RouterLink>
            <RouterLink
              to="/profile"
              class="nav-link px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
              style="color: #8B949E;"
              active-class="nav-link-active"
            >
              Profile
            </RouterLink>
            <div class="ml-2 flex items-center gap-3">
              <span class="text-sm" style="color: #8B949E;">
                {{ auth.currentUser?.username ?? '...' }}
              </span>
              <button
                type="button"
                class="px-3 py-1.5 rounded text-sm font-medium border transition-colors cursor-pointer"
                style="color: #E6EDF3; border-color: #30363D; background: transparent;"
                @click="handleLogout"
                @mouseover="($event.currentTarget as HTMLElement).style.borderColor = '#E6A817'"
                @mouseleave="($event.currentTarget as HTMLElement).style.borderColor = '#30363D'"
              >
                Sign out
              </button>
            </div>
          </template>
          <template v-else>
            <RouterLink
              to="/login"
              class="nav-link px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
              style="color: #8B949E;"
              active-class="nav-link-active"
            >
              Sign in
            </RouterLink>
            <RouterLink
              to="/register"
              class="px-3 py-1.5 rounded text-sm font-semibold transition-colors no-underline"
              style="background-color: #E6A817; color: #0D1117;"
            >
              Get started
            </RouterLink>
          </template>
        </div>

        <!-- Mobile menu button -->
        <button
          class="sm:hidden p-2 rounded transition-colors cursor-pointer"
          style="color: #8B949E; background: transparent; border: none;"
          :aria-expanded="mobileMenuOpen"
          aria-label="Toggle navigation menu"
          @click="toggleMobileMenu"
        >
          <svg v-if="!mobileMenuOpen" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Mobile menu -->
      <div v-if="mobileMenuOpen" class="sm:hidden pb-3 pt-1 border-t" style="border-color: #30363D;">
        <template v-if="auth.isAuthenticated">
          <div class="px-1 py-2 text-sm" style="color: #8B949E;">
            Signed in as <span style="color: #E6EDF3;">{{ auth.currentUser?.username ?? '...' }}</span>
          </div>
          <RouterLink
            v-if="auth.isAdmin"
            to="/admin/users"
            class="block px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
            style="color: #8B949E;"
            active-class="nav-link-active"
            @click="closeMobileMenu"
          >
            User Admin
          </RouterLink>
          <RouterLink
            to="/profile"
            class="block px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
            style="color: #8B949E;"
            active-class="nav-link-active"
            @click="closeMobileMenu"
          >
            Profile
          </RouterLink>
          <button
            type="button"
            class="mt-1 w-full text-left px-3 py-2 rounded text-sm font-medium transition-colors cursor-pointer"
            style="color: #F85149; background: transparent; border: none;"
            @click="handleLogout"
          >
            Sign out
          </button>
        </template>
        <template v-else>
          <RouterLink
            to="/login"
            class="block px-3 py-2 rounded text-sm font-medium transition-colors no-underline"
            style="color: #8B949E;"
            active-class="nav-link-active"
            @click="closeMobileMenu"
          >
            Sign in
          </RouterLink>
          <RouterLink
            to="/register"
            class="block px-3 py-2 rounded text-sm font-medium transition-colors no-underline mt-1"
            style="color: #E6A817;"
            @click="closeMobileMenu"
          >
            Get started
          </RouterLink>
        </template>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.nav-link:hover {
  color: #E6EDF3 !important;
  background-color: #1C2128;
}

.nav-link-active {
  color: #E6A817 !important;
}
</style>
