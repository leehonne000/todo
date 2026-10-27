<template>
  <!-- 模块页顶栏：标题 + 返回（默认回主界面；子页可用 backTo 指定返回目标） -->
  <header class="topbar">
    <button class="back" title="返回" @click="goBack">
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
const props = defineProps({
  title: { type: String, required: true },
  backTo: { type: String, default: '/' } // 返回目标路由（默认主界面）
})
const router = useRouter()
const goBack = () => router.push(props.backTo)
</script>
