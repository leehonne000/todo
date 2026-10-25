<template>
  <div class="timer-root" :class="{ landscape: isLandscapeLike }">
    <!-- iOS 网页锁不了横屏 → 竖屏时整屏引导（产品文档 §4.2） -->
    <div v-if="needRotate" class="rotate-mask">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
        <rect x="7" y="2" width="10" height="20" rx="2" /><path d="M2 12a10 10 0 0 1 4-8" /><path d="M22 12a10 10 0 0 0-4-8" />
      </svg>
      <p>请横过来</p>
    </div>

    <!-- 计时主体：PC 宽卡片 / 手机横屏全屏 -->
    <div class="stage glass-strong">
      <!-- 画面效果层 -->
      <div class="dim-layer" :style="{ opacity: fx.dim / 100 }"></div>
      <div v-if="fx.particles" class="particles">
        <i v-for="p in PARTICLES" :key="p.key" :style="p.style"></i>
      </div>

      <!-- 左上：画面自定义（圈 + 笔），同 Done 的锚定逻辑——跟舞台左缘走 -->
      <button class="fx-btn" title="自定义画面" @click="panelOpen = !panelOpen">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9.2" />
          <path d="M8.6 15.4l1-3.3 5-5 2.3 2.3-5 5-3.3 1z" />
          <path d="M13.5 9.8l2.3 2.3" />
        </svg>
      </button>

      <div class="subject-tag">{{ learn.subjectName }}</div>

      <div class="center">
        <!-- 小字：学习计时器 -->
        <div class="timer-num num">
          <span class="num-inner" :class="{ 'anim-pop': fx.pulse }" :key="fx.pulse ? 't' + timerText : 'tk'">{{ timerText }}</span>
        </div>
        <!-- 大字：当前时间 -->
        <div class="clock-num num" :class="{ glow: fx.glow }">
          <span class="num-inner" :class="{ 'anim-pop': fx.pulse }" :key="fx.pulse ? 'c' + clockText : 'ck'">{{ clockText }}</span>
        </div>
      </div>

      <!-- 画面自定义面板 -->
      <div v-if="panelOpen" class="fx-backdrop" @click="panelOpen = false"></div>
      <transition name="pop">
        <div v-if="panelOpen" class="fx-panel glass-strong">
          <header class="fx-head">
            <span>计时画面</span>
            <button class="fx-x" title="关闭" @click="panelOpen = false">×</button>
          </header>
          <div class="fx-row">
            <span>呼吸光晕</span>
            <button class="switch" :class="{ on: fx.glow }" @click="fx.glow = !fx.glow"><i></i></button>
          </div>
          <div class="fx-row">
            <span>粒子飘动</span>
            <button class="switch" :class="{ on: fx.particles }" @click="fx.particles = !fx.particles"><i></i></button>
          </div>
          <div class="fx-row">
            <span>秒数跳动</span>
            <button class="switch" :class="{ on: fx.pulse }" @click="fx.pulse = !fx.pulse"><i></i></button>
          </div>
          <div class="fx-row">
            <span>背景暗度</span>
            <input class="fx-range" type="range" min="0" max="80" step="5" v-model.number="fx.dim" />
          </div>
        </div>
      </transition>

      <!-- 左下：暂停（圆形 ↔ 方形） -->
      <button class="pause-btn" :class="{ paused }" :title="paused ? '继续' : '暂停'" @click="togglePause">
        <!-- 运行中显示“暂停”双竖条 -->
        <svg v-if="!paused" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16" rx="1"/><rect x="14" y="4" width="4" height="16" rx="1"/></svg>
        <!-- 暂停中显示“播放”三角 -->
        <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
      </button>

      <!-- 右下：长按结束（进度环） -->
      <button
        class="end-btn"
        title="长按结束"
        @pointerdown="startLongPress"
        @pointerup="cancelLongPress"
        @pointerleave="cancelLongPress"
        @pointercancel="cancelLongPress"
      >
        <svg class="ring" viewBox="0 0 64 64">
          <circle class="ring-bg" cx="32" cy="32" r="28" />
          <circle class="ring-fg" cx="32" cy="32" r="28" :style="{ strokeDashoffset: ringOffset }" />
        </svg>
        <span class="end-label">结束</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLearnStore } from '@/stores/learn'
import { useTimerFxStore } from '@/stores/timerFx'
import { finishSession } from '@/api'
import { clockStr, durationStr, pad2 } from '@/utils/format'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const learn = useLearnStore()
const fx = useTimerFxStore()
const panelOpen = ref(false)

// 粒子参数（固定伪随机，避免每次渲染抖动）
const PARTICLES = Array.from({ length: 14 }, (_, i) => ({
  key: i,
  style: {
    left: `${(i * 7 + 5) % 95}%`,
    width: `${2 + (i % 3)}px`,
    height: `${2 + (i % 3)}px`,
    animationDuration: `${9 + (i % 6)}s`,
    animationDelay: `-${((i * 1.7) % 12).toFixed(1)}s`
  }
}))

// ---------- 计时核心 ----------
// 显示与保存共用「墙钟整秒」：计时器和大字时钟都是 floor(now/1000) 的函数，
// 同一帧渲染 → 两个数字严格同刻跳秒。代价：起点量化到整秒，误差 ≤0.5s（可接受）。
// 时间戳推算保留：切后台再回来按真实时间续算，不丢时长。
const nowTick = ref(Date.now())
const paused = ref(false)
const anchorSec = ref(Math.round(Date.now() / 1000)) // 跳秒锚点（整秒索引）
const frozenSec = ref(0)                             // 暂停时冻结的显示秒数

const timerText = computed(() => {
  const nowSec = Math.floor(nowTick.value / 1000)
  const s = Math.max(0, paused.value ? frozenSec.value : nowSec - anchorSec.value)
  return clockStr(s)
})

// 大字时钟与计时器同源（nowTick）→ 同帧更新
const clockText = computed(() => {
  const d = new Date(nowTick.value)
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
})

function currentSec() {
  return Math.max(0, Math.floor(Date.now() / 1000) - anchorSec.value)
}

function togglePause() {
  if (paused.value) {
    // 继续：锚点平移到整秒格上，从冻结处接上
    anchorSec.value = Math.round(Date.now() / 1000) - frozenSec.value
    paused.value = false
  } else {
    // 暂停：冻结当前秒数
    frozenSec.value = currentSec()
    paused.value = true
  }
}

// ---------- 长按结束 ----------
const HOLD_MS = 1200
const holdProgress = ref(0) // 0~1
const ringCircum = 2 * Math.PI * 28
const ringOffset = computed(() => ringCircum * (1 - holdProgress.value))

let holdTimer = null
let holdRaf = null

function startLongPress(e) {
  e.preventDefault()
  const t0 = Date.now()
  cancelLongPress()
  holdTimer = setTimeout(() => {
    holdProgress.value = 1
    onEnd()
  }, HOLD_MS)
  const tick = () => {
    holdProgress.value = Math.min(1, (Date.now() - t0) / HOLD_MS)
    if (holdProgress.value < 1) holdRaf = requestAnimationFrame(tick)
  }
  holdRaf = requestAnimationFrame(tick)
}

function cancelLongPress() {
  clearTimeout(holdTimer)
  cancelAnimationFrame(holdRaf)
  holdProgress.value = 0
}

async function onEnd() {
  if (!paused.value) {
    frozenSec.value = currentSec()
    paused.value = true
  }
  const seconds = frozenSec.value
  try {
    // 契约 §5.3：净学习秒数（排除暂停）
    await finishSession(route.params.sessionId, seconds)
    toast(`本次学习 ${durationStr(seconds)}`)
    router.replace(`/learn/summary/${route.params.sessionId}`)
  } catch (e) {
    toast(e.message)
    // 失败：从冻结处继续跑
    anchorSec.value = Math.round(Date.now() / 1000) - frozenSec.value
    paused.value = false
  }
}

// ---------- 横竖屏判断 ----------
const isLandscapeLike = ref(true)
const needRotate = computed(() => !isLandscapeLike.value)

function checkOrientation() {
  // 只有「触摸设备 + 窄屏 + 竖屏」才提示横屏（手机端产品方案）；
  // PC 窗口无论多竖都不该被挡板拦住
  const coarse = typeof window.matchMedia === 'function' && window.matchMedia('(pointer: coarse)').matches
  const wide = window.innerWidth >= 900
  const landscape = window.innerWidth > window.innerHeight
  isLandscapeLike.value = !coarse || wide || landscape
}

// ---------- 屏幕常亮（有则用，没有拉倒） ----------
let wakeLock = null
async function requestWakeLock() {
  try {
    if (navigator.wakeLock && navigator.wakeLock.request) {
      wakeLock = await navigator.wakeLock.request('screen')
    }
  } catch { /* 不支持就算了 */ }
}

// ---------- 时钟心跳 ----------
let ticker = null
onMounted(() => {
  anchorSec.value = Math.round(Date.now() / 1000)
  requestWakeLock()
  checkOrientation()
  window.addEventListener('resize', checkOrientation)
  window.addEventListener('orientationchange', checkOrientation)
  // 心跳只推 nowTick，两个数字（计时器 + 大字时钟）都从它派生 → 同帧跳秒
  ticker = setInterval(() => {
    nowTick.value = Date.now()
  }, 250)
  // 页面重新可见时，时间戳推算天然正确，无需补偿
  document.addEventListener('visibilitychange', requestWakeLock)
})

onUnmounted(() => {
  clearInterval(ticker)
  window.removeEventListener('resize', checkOrientation)
  window.removeEventListener('orientationchange', checkOrientation)
  document.removeEventListener('visibilitychange', requestWakeLock)
  try { wakeLock && wakeLock.release() } catch { /* ignore */ }
})
</script>

<style scoped>
.timer-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* 计时舞台 */
.stage {
  position: relative;
  width: min(1040px, 94vw);
  aspect-ratio: 16 / 9;
  max-height: 86vh;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 手机横屏：铺满整屏 */
.timer-root.landscape .stage { }
@media (max-width: 899px) {
  .timer-root.landscape { padding: 0; }
  .timer-root.landscape .stage {
    width: 100vw;
    height: 100vh;
    max-height: none;
    aspect-ratio: auto;
    border-radius: 0;
  }
}

.subject-tag {
  position: absolute;
  top: 22px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--text-dim);
}

/* ---------- 画面效果层 ---------- */
.dim-layer {
  position: absolute;
  inset: 0;
  background: #000;
  border-radius: inherit;
  pointer-events: none;
  transition: opacity 0.3s ease;
}
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: inherit;
  pointer-events: none;
}
.particles i {
  position: absolute;
  bottom: -12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.32);
  animation-name: drift;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes drift {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  12% { opacity: 0.55; }
  88% { opacity: 0.3; }
  100% { transform: translateY(-92vh) translateX(26px); opacity: 0; }
}

/* 呼吸光晕（作用于外层，文字阴影继承） */
.clock-num.glow { animation: breathe 3.8s ease-in-out infinite; }
@keyframes breathe {
  0%, 100% { text-shadow: 0 0 16px rgba(255, 255, 255, 0.16); }
  50% { text-shadow: 0 0 44px rgba(255, 255, 255, 0.5); }
}

/* 秒数跳动（作用于数字内层，key 变化重放） */
.num-inner { display: inline-block; }
.num-inner.anim-pop { animation: pop 0.45s ease-out; }
@keyframes pop {
  0% { transform: scale(1.07); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}

/* ---------- 左上自定义按钮（圈 + 笔） ---------- */
.fx-btn {
  position: absolute;
  top: 20px;
  left: 24px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
  transition: background 0.15s;
}
.fx-btn:hover { background: var(--glass-bg-strong); }
.fx-btn svg { width: 21px; height: 21px; }

.fx-backdrop { position: absolute; inset: 0; z-index: 6; }
.fx-panel {
  position: absolute;
  top: 74px;
  left: 24px;
  width: 236px;
  padding: 13px 16px 9px;
  z-index: 7;
}
.fx-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  letter-spacing: 2px;
  margin-bottom: 6px;
}
.fx-x { font-size: 19px; color: var(--text-dim); line-height: 1; }
.fx-x:hover { color: var(--text); }
.fx-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  font-size: 13.5px;
}
.fx-range {
  width: 110px;
  accent-color: var(--theme-text);
}

/* 开关（与自定义页同款） */
.switch {
  width: 40px;
  height: 22px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid var(--glass-border);
  padding: 2px;
  transition: background 0.2s;
}
.switch i {
  display: block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.switch.on { background: var(--theme-deep); }
.switch.on i { transform: translateX(18px); }

.pop-enter-active, .pop-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-6px); }

.center { text-align: center; position: relative; z-index: 2; }
.timer-num {
  font-size: clamp(18px, 2.6vw, 26px);
  color: var(--theme-text);
  letter-spacing: 2px;
  margin-bottom: 10px;
}
.clock-num {
  font-size: clamp(56px, 11vw, 120px);
  font-weight: 200;
  letter-spacing: 4px;
  line-height: 1.05;
}

/* 左下暂停：圆形蓝 → 暂停态变方形 */
.pause-btn {
  position: absolute;
  left: 36px;
  bottom: 32px;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: rgba(72, 128, 224, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-radius 0.22s ease, background 0.22s ease;
}
.pause-btn svg { width: 22px; height: 22px; color: #fff; }
.pause-btn:hover { background: rgba(88, 144, 238, 0.95); }
.pause-btn.paused { border-radius: 16px; background: rgb(var(--theme-rgb) / 0.88); }
.pause-btn.paused svg { color: var(--theme-on, #fff); }

/* 右下结束：长按 + 进度环 */
.end-btn {
  position: absolute;
  right: 36px;
  bottom: 32px;
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
}
.end-btn .ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.ring-bg {
  fill: rgba(224, 74, 74, 0.22);
  stroke: rgba(255, 255, 255, 0.18);
  stroke-width: 3;
}
.ring-fg {
  fill: none;
  stroke: #e24b4a;
  stroke-width: 3.5;
  stroke-linecap: round;
  stroke-dasharray: 175.9;
  stroke-dashoffset: 175.9;
}
.end-label {
  position: relative;
  font-size: 14px;
  letter-spacing: 3px;
  color: #fff;
}

/* 竖屏引导遮罩 */
.rotate-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(8, 10, 22, 0.94);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
}
.rotate-mask svg { width: 64px; height: 64px; color: var(--theme-text); animation: rot 1.6s ease-in-out infinite; }
.rotate-mask p { font-size: 19px; letter-spacing: 6px; }
@keyframes rot {
  0%, 20% { transform: rotate(0); }
  60%, 100% { transform: rotate(90deg); }
}
</style>
