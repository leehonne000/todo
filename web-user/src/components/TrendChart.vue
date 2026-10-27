<template>
  <!-- 轻量柱状/折线图：无第三方库，SVG 手绘，风格对齐贡献墙 -->
  <div ref="wrap" class="chart-wrap">
    <p v-if="isEmpty" class="empty dim">暂无数据</p>
    <svg v-else-if="w > 0" class="chart" :width="w" :height="H" :viewBox="`0 0 ${w} ${H}`">
      <!-- 网格线 + 纵轴刻度（0 / 半 / 满） -->
      <g>
        <line v-for="g in yGrid" :key="'g' + g.v" class="gl"
              :x1="PAD_L" :x2="w - PAD_R" :y1="g.y" :y2="g.y" />
        <text v-for="g in yGrid" :key="'t' + g.v" class="lab ylab"
              :x="PAD_L - 8" :y="g.y + 4">{{ g.label }}</text>
      </g>

      <!-- 柱状 -->
      <template v-if="type === 'bar'">
        <rect v-for="b in bars" :key="b.date" class="bar"
              :x="b.x" :y="b.y" :width="b.bw" :height="b.h" rx="3"
              :style="{ fill: color }">
          <title>{{ b.tip }}</title>
        </rect>
      </template>

      <!-- 折线 -->
      <template v-else>
        <polyline v-if="linePath" class="line" :points="linePath" :style="{ stroke: color }" />
        <circle v-for="p in linePts" :key="p.date" :cx="p.x" :cy="p.y" r="3.5"
                :style="{ fill: color }">
          <title>{{ p.tip }}</title>
        </circle>
      </template>

      <!-- 横轴日期 -->
      <text v-for="l in xLabels" :key="'x' + l.date" class="lab xlab" :x="l.x" :y="H - 8">{{ l.text }}</text>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // [{ date: 'YYYY-MM-DD', value: Number, future: Boolean }]
  points: { type: Array, default: () => [] },
  type: { type: String, default: 'bar' }, // 'bar' | 'line'
  color: { type: String, default: 'var(--theme-text)' }, // CSS 颜色值
  unit: { type: String, default: '' } // 悬浮提示单位（分钟/条）
})

const H = 230
const PAD_L = 40
const PAD_R = 10
const PAD_T = 14
const PAD_B = 30

const wrap = ref(null)
const w = ref(0)
let ro = null

onMounted(() => {
  ro = new ResizeObserver(() => { w.value = wrap.value?.clientWidth || 0 })
  ro.observe(wrap.value)
  w.value = wrap.value?.clientWidth || 0
})
onBeforeUnmount(() => ro?.disconnect())

// 有效数据（未来天不画）
const live = computed(() => props.points.filter(p => !p.future))
const isEmpty = computed(() => live.value.reduce((a, p) => a + (p.value || 0), 0) === 0)

// 纵轴上限取个好看的整数
function niceMax(v) {
  if (v <= 5) return 5
  const pow = Math.pow(10, Math.floor(Math.log10(v)))
  const unit = pow / 2
  return Math.ceil((v * 1.08) / unit) * unit
}
const maxVal = computed(() => niceMax(Math.max(0, ...live.value.map(p => p.value || 0))))

const plotW = computed(() => Math.max(0, w.value - PAD_L - PAD_R))
const plotH = H - PAD_T - PAD_B
const slot = computed(() => (props.points.length ? plotW.value / props.points.length : 0))
const xOf = (i) => PAD_L + slot.value * (i + 0.5)
const yOf = (v) => PAD_T + plotH * (1 - v / maxVal.value)

const yGrid = computed(() => {
  const m = maxVal.value
  return [
    { v: m, y: yOf(m), label: String(Math.round(m)) },
    { v: m / 2, y: yOf(m / 2), label: String(Math.round(m / 2)) },
    { v: 0, y: yOf(0), label: '0' }
  ]
})

const tipOf = (p) => `${p.date} · ${p.value || 0} ${props.unit}`

const bars = computed(() => props.points.map((p, i) => {
  const bw = Math.min(slot.value * 0.58, 26)
  const v = p.future ? 0 : (p.value || 0)
  return {
    date: p.date,
    x: xOf(i) - bw / 2,
    bw,
    h: v > 0 ? Math.max(2, plotH * (v / maxVal.value)) : 0,
    y: yOf(v),
    tip: tipOf(p)
  }
}).filter(b => b.h > 0))

const linePts = computed(() => props.points.map((p, i) => ({
  date: p.date,
  x: xOf(i),
  y: yOf(p.future ? 0 : (p.value || 0)),
  tip: tipOf(p)
})).filter((_, i) => !props.points[i].future))

const linePath = computed(() => linePts.value.map(p => `${p.x},${p.y}`).join(' '))

const xLabels = computed(() => {
  const n = props.points.length
  const step = n <= 8 ? 1 : Math.ceil(n / 7)
  return props.points.map((p, i) => ({
    date: p.date,
    x: xOf(i),
    // 短区间显示 M-D，长区间只显示日号
    text: n <= 8 ? p.date.slice(5) : String(Number(p.date.slice(8, 10)))
  })).filter((_, i) => i % step === 0)
})
</script>

<style scoped>
.chart-wrap { width: 100%; }
.chart { display: block; }
.gl {
  stroke: rgba(255, 255, 255, 0.12);
  stroke-width: 1;
}
.lab {
  font-size: 11px;
  fill: var(--text-faint);
}
.ylab { text-anchor: end; }
.xlab { text-anchor: middle; }
.bar { transition: opacity 0.15s; }
.bar:hover { opacity: 0.8; }
.line {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.empty { text-align: center; padding: 36px 0; font-size: 13.5px; }
</style>
