<template>
  <div class="page page-narrow">
    <TopBar title="学习趋势" />

    <!-- 筛选：时间范围 / 图表类型（全站统一下划线 tab） -->
    <section class="filters glass">
      <div class="f-group">
        <span class="f-label">时间范围</span>
        <TabRow v-model="range" :items="rangeItems" />
      </div>
      <div class="f-group">
        <span class="f-label">图表类型</span>
        <TabRow v-model="chartType" :items="typeItems" />
      </div>
    </section>

    <!-- 图一：学习时间 -->
    <section class="chart-card glass">
      <header class="card-head">
        <h2 class="card-title">学习时间</h2>
        <span class="dim">{{ rangeLabel }}共 <b class="amber num">{{ totalDuration }}</b></span>
      </header>
      <TrendChart :points="timePoints" :type="chartType" :color="TIME_COLOR" unit="分钟" />
    </section>

    <!-- 图二：记录数（note + 错题合并统计） -->
    <section class="chart-card glass">
      <header class="card-head">
        <h2 class="card-title">记录数（note + 错题）</h2>
        <span class="dim">{{ rangeLabel }}共 <b class="coral num">{{ totalRecords }}</b> 条</span>
      </header>
      <TrendChart :points="countPoints" :type="chartType" :color="COUNT_COLOR" unit="条" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import TabRow from '@/components/TabRow.vue'
import TrendChart from '@/components/TrendChart.vue'
import { getProgress } from '@/api'
import { studyDate, parseDate, addDaysStr, pad2, durationStr } from '@/utils/format'
import { toast } from '@/utils/toast'

const TIME_COLOR = 'var(--theme-text)'
const COUNT_COLOR = '#f0997b'

const rangeItems = [
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' }
]
const typeItems = [
  { value: 'bar', label: '柱状图' },
  { value: 'line', label: '折线图' }
]

const range = ref('week')
const chartType = ref('bar')
const days = ref([])

// 学习日口径的「今天」
const todayStr = studyDate()
const rangeLabel = computed(() => (range.value === 'week' ? '本周' : '本月'))

// 本周=周一~周日；本月=1 日~月末（未来天留空不画）
function rangeBounds() {
  const today = parseDate(todayStr)
  if (range.value === 'week') {
    const dowMon = (today.getDay() + 6) % 7 // 周一=0
    const monday = addDaysStr(todayStr, -dowMon)
    return { from: monday, to: addDaysStr(monday, 6) }
  }
  const y = today.getFullYear()
  const m = today.getMonth() + 1
  const lastDay = new Date(y, m, 0).getDate()
  return { from: `${y}-${pad2(m)}-01`, to: `${y}-${pad2(m)}-${pad2(lastDay)}` }
}

async function load() {
  const { from, to } = rangeBounds()
  try {
    const data = await getProgress(from, to)
    days.value = data.days
  } catch (e) {
    toast(e.message)
  }
}

const timePoints = computed(() => days.value.map(d => ({
  date: d.date,
  value: Math.round(d.totalSeconds / 60), // 按分钟画图
  future: d.date > todayStr
})))

// note + 错题统一在一起统计
const countPoints = computed(() => days.value.map(d => ({
  date: d.date,
  value: d.noteCount + d.wrongCount,
  future: d.date > todayStr
})))

const totalSeconds = computed(() => days.value.reduce((a, d) => a + d.totalSeconds, 0))
const totalDuration = computed(() => durationStr(totalSeconds.value))
const totalRecords = computed(() => countPoints.value.reduce((a, p) => a + p.value, 0))

watch(range, load)
onMounted(load)
</script>

<style scoped>
.filters {
  width: 100%;
  padding: 18px 28px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 32px;
}
.f-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12.5px;
  letter-spacing: 1px;
  color: var(--text-faint);
}

.chart-card {
  width: 100%;
  padding: 22px 28px;
  margin-top: var(--gap-section);
}
.chart-card:last-child { margin-bottom: 30px; }

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card-title { font-size: 16px; font-weight: 500; letter-spacing: 2px; }
.card-head .dim { font-size: 13px; }
.card-head b { font-size: 15px; font-weight: 500; }
.coral { color: #f0997b; }

@media (max-width: 640px) {
  .filters { grid-template-columns: 1fr; gap: 14px; }
}
@media (min-width: 900px) {
  .chart-card { padding: 28px 32px; }
}
</style>
