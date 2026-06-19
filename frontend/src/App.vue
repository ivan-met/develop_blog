<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: #0D1117; color: #E6EDF3;">
    <NavBar />
    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>
