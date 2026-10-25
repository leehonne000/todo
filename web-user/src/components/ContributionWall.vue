<template>
  <!-- 学习贡献墙：GitHub 贡献图样式，一格一天，琥珀色阶按当日学习时长分级 -->
  <section class="wall glass">
    <header class="w-head">
      <h2 class="w-title">学习贡献</h2>
      <span v-if="streak > 0" class="w-streak">
        连续学习 <b class="amber num">{{ streak }}</b> 天
      </span>
      <span class="dim w-total">近 26 周共 <b class="amber num">{{ activeDays }}</b> 天有学习记录</span>
    </header>

    <div class="grid-wrap">
      <!-- 月份标注：与格子同列宽的网格，按周索引定位 -->
      <div class="months">
        <span
          v-for="m in monthLabels" :key="m.key"
          class="month" :style="{ gridColumnStart: m.weekIndex + 1 }"
        >{{ m.label }}</span>
      </div>

      <div class="grid-row">
        <!-- 周几标注（与行同高） -->
        <div class="day-labels">
          <span class="dl" style="grid-row: 1">一</span>
          <span class="dl" style="grid-row: 3">三</span>
          <span class="dl" style="grid-row: 5">五</span>
        </div>
        <!-- 26 周 × 7 天 -->
        <div class="cells">
          <div v-for="(week, wi) in weeks" :key="wi" class="week">
            <div
              v-for="cell in week"
              :key="cell.date || wi + '-' + Math.random()"
              class="cell"
              :class="cell.level ? 'l' + cell.level : 'empty'"
              :title="cell.tip"
            ></div>
          </div>
        </div>
      </div>

      <!-- 图例 -->
      <div class="legend">
        <span class="dim">少</span>
        <i class="cell empty"></i>
        <i class="cell l1"></i>
        <i class="cell l2"></i>
        <i class="cell l3"></i>
        <i class="cell l4"></i>
        <span class="dim">多</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getProgress } from '@/api'
import { dateStr, addDaysStr, durationStr, parseDate, daysFromToday, studyDate } from '@/utils/format'

const dayMap = ref(new Map()) // 'YYYY-MM-DD' → totalSeconds
const weeks = ref([])
const streak = ref(0)

const activeDays = computed(() => [...dayMap.value.values()].filter(s => s > 0).length)

const monthLabels = computed(() => {
  const labels = []
  let lastMonth = -1
  weeks.value.forEach((week, i) => {
    const first = week.find(c => c.date)
    if (!first) return
    const m = parseDate(first.date).getMonth()
    if (m !== lastMonth && i < weeks.value.length - 1) {
      lastMonth = m
      labels.push({ key: first.date, label: `${m + 1}月`, weekIndex: i })
    }
  })
  return labels
})

/** 当日学习时长 → 0~4 级（对齐 GitHub 的四级绿阶，换成琥珀） */
function levelOf(sec) {
  if (!sec) return 0
  if (sec < 3600) return 1   // <1h
  if (sec < 7200) return 2   // 1~2h
  if (sec < 14400) return 3  // 2~4h
  return 4                   // 4h+
}

onMounted(async () => {
  const today = studyDate() // 学习日「今天」（凌晨 4 点前算前一天）
  // 26 周 = 182 天；对齐到“周”起点（周一）
  const todayDow = (parseDate(today).getDay() + 6) % 7 // 周一=0
  const endOfThisWeek = addDaysStr(today, 6 - todayDow)
  const startOfGrid = addDaysStr(endOfThisWeek, -(26 * 7 - 1))

  try {
    const data = await getProgress(startOfGrid, today)
    for (const d of data.days) dayMap.value.set(d.date, d.totalSeconds)
  } catch { /* 贡献墙失败不阻塞页面 */ }

  // 组装 26 周 × 7 天网格
  const grid = []
  let cursor = startOfGrid
  for (let w = 0; w < 26; w++) {
    const week = []
    for (let d = 0; d < 7; d++) {
      const future = daysFromToday(cursor) > 0
      const sec = dayMap.value.get(cursor) || 0
      week.push({
        date: future ? null : cursor,
        level: future ? 0 : levelOf(sec),
        tip: future ? '' : `${cursor} · ${sec ? durationStr(sec) : '没学习'}`
      })
      cursor = addDaysStr(cursor, 1)
    }
    grid.push(week)
  }
  weeks.value = grid

  // 连续学习天数：从今天（或昨天，今天还没学时）往回数
  let n = 0
  let probe = (dayMap.value.get(today) || 0) > 0 ? today : addDaysStr(today, -1)
  while ((dayMap.value.get(probe) || 0) > 0) {
    n += 1
    probe = addDaysStr(probe, -1)
  }
  streak.value = n
})
</script>

<style scoped>
.wall {
  width: 100%;
  padding: 22px 28px 18px;
  margin-top: var(--gap-section);
}

.w-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.w-title { font-size: 16px; font-weight: 500; letter-spacing: 2px; }
.w-streak { font-size: 13px; }
.w-streak b { font-size: 16px; font-weight: 500; }
.w-total { margin-left: auto; font-size: 12.5px; }
.w-total b { font-weight: 500; }

/* 格子尺寸随窗口伸缩（PC 放大到 18px，手机 12px） */
.grid-wrap {
  --cw: clamp(12px, 1.5vw, 18px);
  --cg: clamp(2px, 0.35vw, 4px);
  overflow-x: auto;
  padding-bottom: 4px;
}

/* 月份标注：26 列网格，与格子同列宽 */
.months {
  display: grid;
  grid-template-columns: repeat(26, var(--cw));
  gap: var(--cg);
  margin-left: calc(var(--cw) + 8px);
  margin-bottom: 5px;
}
.month {
  font-size: 11px;
  color: var(--text-faint);
  white-space: nowrap;
}

.grid-row { display: flex; gap: 8px; }

/* 周几标注：7 行网格，与格子同行高 */
.day-labels {
  display: grid;
  grid-template-rows: repeat(7, var(--cw));
  gap: var(--cg);
  flex: none;
}
.dl {
  font-size: 10px;
  color: var(--text-faint);
  line-height: var(--cw);
  text-align: right;
  width: 14px;
}

.cells { display: flex; gap: var(--cg); }
.week { display: flex; flex-direction: column; gap: var(--cg); }

.cell {
  width: var(--cw);
  height: var(--cw);
  border-radius: 3px;
  display: inline-block;
  flex: none;
}
.cell.empty { background: rgba(255, 255, 255, 0.08); }
.cell.l1 { background: rgb(var(--theme-text-rgb) / 0.28); }
.cell.l2 { background: rgb(var(--theme-text-rgb) / 0.5); }
.cell.l3 { background: rgb(var(--theme-text-rgb) / 0.72); }
.cell.l4 { background: var(--theme-text); }

.legend {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  font-size: 11px;
}
.legend .cell { width: 11px; height: 11px; }
</style>
