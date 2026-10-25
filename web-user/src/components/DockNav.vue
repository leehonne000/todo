<template>
  <!-- 底部三模块入口（信息 / 复习 / 进度）。只出现在主界面，模块内用 topbar 返回 -->
  <nav class="dock glass">
    <button class="dock-btn" :class="{ active: active === 'info' }" @click="$router.push('/info')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
      </svg>
      <span>信息</span>
    </button>
    <button class="dock-btn" :class="{ active: active === 'library' }" @click="$router.push('/library')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" /><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
      </svg>
      <span>复习</span>
    </button>
    <button class="dock-btn" :class="{ active: active === 'progress' }" @click="$router.push('/progress')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
      </svg>
      <span>进度</span>
    </button>
  </nav>
</template>

<script setup>
defineProps({ active: { type: String, default: '' } })
</script>
