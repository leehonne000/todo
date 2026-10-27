<template>
  <div>
    <!-- 双层壁纸：current 常驻，incoming 淡入覆盖后完成交接（交叉过渡） -->
    <div class="wallpaper">
      <!-- wp-shift：鼠标视差位移层（动效强度控制幅度），两层壁纸一起动 -->
      <div class="wp-shift" :style="shiftStyle">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from '@/utils/toast'
import { useWallpaperStore } from '@/stores/wallpaper'
import { useThemeStore } from '@/stores/theme'
const toast = useToast()
const wp = useWallpaperStore()
// 启动即把主题色写进 :root（刷新后保持用户选的主题）
useThemeStore()
// 每日更新壁纸（开关开启且今天还没换过才生效）
onMounted(() => {
  wp.dailyCheck()
  window.addEventListener('mousemove', onMouse, { passive: true })
  document.documentElement.addEventListener('mouseleave', onMouseLeave)
})
onUnmounted(() => {
  window.removeEventListener('mousemove', onMouse)
  document.documentElement.removeEventListener('mouseleave', onMouseLeave)
})

// ---------- 壁纸动效强度（鼠标视差） ----------
// 鼠标归一化到 -1..1（屏幕中心为 0）；动效强度 0~100 映射最大位移 0~32px
const nx = ref(0)
const ny = ref(0)
const MAX_SHIFT = 32

function onMouse(e) {
  nx.value = (e.clientX / window.innerWidth) * 2 - 1
  ny.value = (e.clientY / window.innerHeight) * 2 - 1
}
// 指针离开窗口：缓慢回中，避免壁纸卡在边角
function onMouseLeave() {
  nx.value = 0
  ny.value = 0
}

// 反向视差（鼠标向右 → 壁纸向左），画面有“景深往后退”的感觉
const shiftStyle = computed(() => {
  const amp = (wp.motion / 100) * MAX_SHIFT
  return {
    transform: `translate3d(${(-nx.value * amp).toFixed(1)}px, ${(-ny.value * amp).toFixed(1)}px, 0)`
  }
})
</script>

<style scoped>
.wallpaper { position: fixed; inset: 0; z-index: -2; overflow: hidden; }
/* 视差位移层：超出视口一圈（±56px > 最大位移 32px），移动时不露边 */
.wp-shift {
  position: absolute;
  inset: -56px;
  transition: transform 0.35s ease-out;
  will-change: transform;
}
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
