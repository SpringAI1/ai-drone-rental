<template>
  <router-view />
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

authStore.initAuth()
</script>

<style lang="scss">
// 全局样式已在main.js中引入
</style>
