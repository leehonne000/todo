<template>
  <!-- 模块页顶栏：标题 + 返回主界面（hub-and-spoke：退出模块永远回根屏） -->
  <header class="topbar">
    <button class="back" title="返回主界面" @click="goHome">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 12H5" /><path d="M12 19l-7-7 7-7" />
      </svg>
    </button>
    <h1>{{ title }}</h1>
    <slot />
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
defineProps({ title: { type: String, required: true } })
const router = useRouter()
const goHome = () => router.push('/')
</script>
