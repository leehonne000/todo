<template>
  <div>
    <!-- 双层壁纸：current 常驻，incoming 淡入覆盖后完成交接（交叉过渡） -->
    <div class="wallpaper">
      <div
        class="wp-layer"
        :style="{ backgroundImage: `url(${wp.current})`, filter: wp.filterCss, transform: wp.fading ? 'scale(1.04)' : 'scale(1)' }"
      ></div>
      <div
        class="wp-layer"
        :class="{ show: wp.fading }"
        :style="{ backgroundImage: `url(${wp.incoming})`, filter: wp.filterCss }"
      ></div>
    </div>
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <transition name="fade">
      <div v-if="toast.show" class="toast">{{ toast.text }}</div>
    </transition>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useToast } from '@/utils/toast'
import { useWallpaperStore } from '@/stores/wallpaper'
import { useThemeStore } from '@/stores/theme'
const toast = useToast()
const wp = useWallpaperStore()
// 启动即把主题色写进 :root（刷新后保持用户选的主题）
useThemeStore()
// 每日更新壁纸（开关开启且今天还没换过才生效）
onMounted(() => wp.dailyCheck())
</script>

<style scoped>
.wallpaper { position: fixed; inset: 0; z-index: -2; overflow: hidden; }
.wp-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  transition: opacity 0.6s ease, transform 0.9s ease;
  opacity: 1;
}
/* 入场层：从透明淡入，同时底图轻微放大 → 有“换景”的呼吸感 */
.wp-layer.show { opacity: 1; }
.wp-layer:last-child { opacity: 0; }
.wp-layer:last-child.show { opacity: 1; }
/* 压暗遮罩：保证白字可读 */
.wallpaper::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 26, 0.42);
  z-index: 1;
}
</style>
