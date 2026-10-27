<template>
  <div class="page page-narrow">
    <!-- 统计 -->
    <TopBar title="统计" />

    <!-- 概览：2×2 大数字 -->
    <section class="overview glass">
      <div class="ov-item">
        <div class="ov-label">
          <svg class="ov-ic amber-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M4 6h16M4 12h10M4 18h13" />
          </svg>
          <span>今日学习&复习</span>
        </div>
        <div class="ov-num"><b class="num amber">{{ todayCount }}</b><i>条</i></div>
      </div>

      <div class="ov-item">
        <div class="ov-label">
          <svg class="ov-ic coral-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 17l6-6 4 4 8-8" /><path d="M21 7v5h-5" />
          </svg>
          <span>累计学习</span>
        </div>
        <div class="ov-num"><b class="num">{{ totalCount }}</b><i>条</i></div>
      </div>

      <div class="ov-item">
        <div class="ov-label">
          <svg class="ov-ic amber-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" stroke-linecap="round" />
          </svg>
          <span>今日时长</span>
        </div>
        <div class="ov-num"><b class="num amber">{{ todayMinutes }}</b><i>分钟</i></div>
      </div>

      <div class="ov-item">
        <div class="ov-label">
          <svg class="ov-ic coral-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" stroke-linecap="round" />
          </svg>
          <span>累计时长</span>
        </div>
        <div class="ov-num"><b class="num">{{ totalMinutes }}</b><i>分钟</i></div>
      </div>
    </section>

    <!-- 签到日历：本周条 + 连续签到 -->
    <section class="checkin-card glass">
      <header class="card-head">
        <h2 class="card-title">签到日历</h2>
        <!-- 连续签到=学习趋势页入口（柱状/折线 × 本周/本月） -->
        <button class="streak dim" @click="$router.push('/progress/trend')">
          连续签到 <b class="amber num">{{ streak }}</b> 天 ›
        </button>
      </header>
      <div class="week">
        <div v-for="d in weekDays" :key="d.key" class="day">
          <span class="dow" :class="{ today: d.isToday }">{{ d.dow }}</span>
          <span class="date" :class="{ checked: d.checked, today: d.isToday }">
            {{ d.isToday ? '今' : d.dayNum }}
          </span>
        </div>
      </div>
    </section>

    <!-- 学习贡献墙（GitHub 贡献图效果） -->
    <ContributionWall />

    <!-- 各科累计耗时（紧凑条形，替代原逐日列表） -->
    <section class="subjects glass">
      <h2 class="card-title">各科累计耗时</h2>
      <div v-for="s in subjectStats" :key="s.subjectName" class="s-row">
        <span class="s-name">{{ s.subjectName }}</span>
        <div class="s-track">
          <div class="s-fill" :style="{ width: s.pct + '%', background: colorOf(s.subjectName) }"></div>
        </div>
        <span class="s-sec dim num">{{ durationStr(s.seconds) }}</span>
      </div>
      <p v-if="!subjectStats.length" class="empty dim">还没有学习记录</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import ContributionWall from '@/components/ContributionWall.vue'
import { getProgress } from '@/api'
import { dateStr, addDaysStr, durationStr, parseDate, pad2, daysFromToday, studyDate } from '@/utils/format'
import { toast } from '@/utils/toast'

const days = ref([])
const checkInSet = ref(new Set())
// 学习日「今天」（凌晨 4 点前仍算前一天）
const todayStr = studyDate()

// ---------- 概览 2×2 ----------
const todayCount = computed(() => {
  const d = days.value.find(x => x.date === todayStr)
  return d ? d.noteCount + d.wrongCount : 0
})
const totalCount = computed(() => days.value.reduce((a, d) => a + d.noteCount + d.wrongCount, 0))
const todayMinutes = computed(() => {
  const d = days.value.find(x => x.date === todayStr)
  return d ? Math.round(d.totalSeconds / 60) : 0
})
const totalMinutes = computed(() => Math.round(days.value.reduce((a, d) => a + d.totalSeconds, 0) / 60))

// ---------- 签到日历：本周一 ~ 周日（按学习日所在周） ----------
const weekDays = computed(() => {
  const dowMon = (parseDate(todayStr).getDay() + 6) % 7 // 周一=0
  const monday = addDaysStr(todayStr, -dowMon)
  const names = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
  return names.map((dow, i) => {
    const date = addDaysStr(monday, i)
    return {
      key: date,
      dow,
      dayNum: String(parseDate(date).getDate()),
      isToday: date === todayStr,
      checked: checkInSet.value.has(date)
    }
  })
})

// 连续签到：从今天（今天没签则从昨天）往前数
const streak = computed(() => {
  let start = checkInSet.value.has(todayStr) ? todayStr : addDaysStr(todayStr, -1)
  let n = 0
  let cur = start
  while (checkInSet.value.has(cur)) {
    n += 1
    cur = addDaysStr(cur, -1)
  }
  return n
})

// ---------- 各科累计 ----------
const subjectStats = computed(() => {
  const map = new Map()
  for (const d of days.value) {
    for (const b of d.bySubject) {
      map.set(b.subjectName, (map.get(b.subjectName) || 0) + b.seconds)
    }
  }
  const arr = [...map.entries()]
    .map(([subjectName, seconds]) => ({ subjectName, seconds }))
    .sort((a, b) => b.seconds - a.seconds)
  const max = arr.length ? arr[0].seconds : 1
  return arr.map(s => ({ ...s, pct: Math.max(4, Math.round((s.seconds / max) * 100)) }))
})

// 每科一个稳定颜色
const PALETTE = ['#fac775', '#5dcaa5', '#85b7eb', '#afa9ec', '#f0997b', '#f4c0d1']
function colorOf(name) {
  let h = 0
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) % 997
  return PALETTE[h % PALETTE.length]
}

onMounted(async () => {
  try {
    // 一次拉全量（近 400 天）：概览累计 + 签到 + 各科累计都从这份算
    const data = await getProgress(addDaysStr(todayStr, -400), todayStr)
    days.value = data.days
    checkInSet.value = new Set(data.checkInDates || [])
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
/* 连续签到入口（进学习趋势页） */
.streak {
  font-size: 13px;
  padding: 4px 2px;
  background: none;
  border: none;
  cursor: pointer;
}
.streak:hover { color: var(--theme-text); }
.streak b { font-size: 15px; font-weight: 500; }

/* 概览 2×2 */
.overview {
  width: 100%;
  padding: 26px 28px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 26px 20px;
}
.ov-label {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13.5px;
  color: var(--text-dim);
  letter-spacing: 1px;
}
.ov-ic { width: 19px; height: 19px; flex: none; }
.amber-ic { color: var(--theme-text); }
.coral-ic { color: #f0997b; }
.ov-num {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  gap: 7px;
}
.ov-num b {
  font-size: clamp(34px, 4.5vw, 46px);
  font-weight: 500;
  line-height: 1;
  color: var(--text);
}
.ov-num b.amber { color: var(--theme-text); }
.ov-num i {
  font-style: normal;
  font-size: 13px;
  color: var(--text-faint);
}

/* 签到日历 */
.checkin-card { width: 100%; padding: 22px 28px; margin-top: var(--gap-section); }
.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.card-title { font-size: 16px; font-weight: 500; letter-spacing: 2px; }

.week {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 11px;
}
.dow {
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--text-faint);
}
.dow.today { color: var(--theme-text); }
.date {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: var(--text-dim);
}
.date.checked {
  background: var(--amber-deep);
  color: var(--theme-on);
  font-weight: 600;
}
.date.today { border: 1.5px solid rgb(var(--theme-rgb) / 0.6); color: var(--theme-text); }
.date.checked.today { color: var(--theme-on); border-color: transparent; }

/* 各科累计 */
.subjects { width: 100%; padding: 22px 28px; margin-top: var(--gap-section); margin-bottom: 30px; }
.s-row {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-top: 13px;
}
.s-name { width: 72px; font-size: 13px; color: var(--text-dim); flex: none; }
.s-track {
  flex: 1;
  height: 9px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}
.s-fill { height: 100%; border-radius: 5px; transition: width 0.4s ease; }
.s-sec { width: 90px; text-align: right; font-size: 12.5px; flex: none; }

.empty { text-align: center; padding: 20px 0; font-size: 13.5px; }

@media (min-width: 900px) {
  .overview { padding: 32px 38px; gap: 32px 28px; }
  .date { width: 46px; height: 46px; }
}
</style>
