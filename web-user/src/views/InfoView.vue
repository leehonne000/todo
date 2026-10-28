<template>
  <div class="page page-narrow">
    <TopBar title="信息" />

    <!-- 固定信息区：倒计时 -->
    <section class="countdown glass-strong">
      <p class="label dim">距离 {{ overview.examYear }} 年考研还有</p>
      <p class="days num amber">{{ overview.daysLeft }}</p>
      <p class="label dim">天</p>
    </section>

    <!-- 时间轴：一根轴 + 比例间距的节点 + 当前日期节点（可横向滚动） -->
    <section v-if="tl" class="milestones glass">
      <div class="tl-scroll">
        <div class="tl" :style="{ width: tl.width + 'px' }">
          <!-- 当前日期徽标 -->
          <div class="tl-top">
            <span class="tl-today-badge" :style="{ left: tl.todayX + 'px' }">{{ tl.todayLabel }}</span>
          </div>

          <!-- 上排节点标签 -->
          <div class="tl-row up">
            <span
              v-for="r in tl.above" :key="'u' + r.name"
              class="tl-label" :class="{ active: r.name === tl.activeName, past: r.past }"
              :style="{ left: r.x + 'px' }"
            >
              <b>{{ r.name }}</b>
              <i>{{ r.range }}</i>
            </span>
          </div>

          <!-- 轴 -->
          <div class="tl-axis">
            <div class="tl-base"></div>
            <div
              v-for="r in tl.rows" :key="'s' + r.name"
              class="tl-seg" :class="{ active: r.name === tl.activeName, past: r.past }"
              :style="{ left: r.segLeft + 'px', width: r.segWidth + 'px' }"
            >
              <span class="tl-dot"></span>
              <title>{{ r.name }} · {{ r.range }}</title>
            </div>
            <!-- 当前日期 -->
            <div class="tl-today-line" :style="{ left: tl.todayX + 'px' }"></div>
            <div class="tl-today-mark" :style="{ left: tl.todayX + 'px' }"></div>
          </div>

          <!-- 下排节点标签 -->
          <div class="tl-row down">
            <span
              v-for="r in tl.below" :key="'d' + r.name"
              class="tl-label" :class="{ active: r.name === tl.activeName, past: r.past }"
              :style="{ left: r.x + 'px' }"
            >
              <b>{{ r.name }}</b>
              <i>{{ r.range }}</i>
            </span>
          </div>

          <!-- 月份刻度 -->
          <div class="tl-months">
            <span v-for="mo in tl.months" :key="'m' + mo.x + mo.label" class="tl-month" :style="{ left: mo.x + 'px' }">
              <em></em>{{ mo.label }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- 博客内容区（超级管理员发布，用户只读） -->
    <section class="articles">
      <h2 class="sec-title">博客</h2>
      <article
        v-for="a in articles" :key="a.id"
        class="article-card glass" @click="$router.push(`/info/article/${a.id}`)"
      >
        <div class="a-main">
          <h3 class="a-title">{{ a.title }}<span v-if="unreadArticleIds.has(a.id)" class="a-badge" title="有未读回复"></span></h3>
          <p class="a-date dim num">{{ (a.publishedAt || '').replace('T', ' ').slice(0, 10) }}<span v-if="a.authorName" class="a-author"> · {{ a.authorName }}</span></p>
        </div>
        <img v-if="a.coverUrl" class="a-cover" :src="a.coverUrl" alt="" />
      </article>
      <p v-if="!articles.length" class="empty dim">还没有发布文章</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import { getInfoOverview, getArticles, getUnreadMessages } from '@/api'
import { studyDate } from '@/utils/format'
import { toast } from '@/utils/toast'

const overview = ref({ examYear: '', daysLeft: 0, milestones: [] })
const articles = ref([])
// 有未读回复的文章（读者来信红点定位，2026-10-28）
const unreadArticleIds = ref(new Set())

// 学习日「今天」
const todayStr = studyDate()

function fmtRange(m) {
  const f = m.dateFrom.slice(5).replace('-', '/')
  const t = m.dateTo ? m.dateTo.slice(5).replace('-', '/') : ''
  return t && t !== f ? `${f} – ${t}` : f
}

const toDate = (s) => {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

// 轴比例：3px/天（可横向滚动），日期差值→像素间距，比例严格
const PX = 3
const PAD = 64

/**
 * 横向时间轴：一根轴线，节点（区间胶囊）按真实日期比例排布，
 * 标签上下交错；「当前日期」为独立节点（虚线 + 菱形 + 徽标）。
 */
const tl = computed(() => {
  const ms = overview.value.milestones || []
  if (!ms.length) return null
  const DAY = 86400000
  const tToday = toDate(todayStr).getTime()
  const times = ms.flatMap(m => [toDate(m.dateFrom).getTime(), toDate(m.dateTo || m.dateFrom).getTime()])
  const t0 = Math.min(tToday, ...times)
  const t1 = Math.max(tToday, ...times)
  const x = (t) => PAD + Math.round((t - t0) / DAY) * PX

  const rows = ms.map((m, i) => {
    const f = toDate(m.dateFrom).getTime()
    const t = toDate(m.dateTo || m.dateFrom).getTime()
    return {
      name: m.name,
      range: fmtRange(m),
      x: x(f + (t - f) / 2), // 标签与圆点锚在区间中点
      segLeft: x(f),
      segWidth: Math.max(Math.round((t - f) / DAY) * PX + PX, 6),
      above: i % 2 === 0,
      past: (m.dateTo || m.dateFrom) < todayStr
    }
  })

  const cur = ms.find(m => (m.dateTo || m.dateFrom) >= todayStr)

  // 月份刻度（按真实月份起点定位；起点落在轴外的不标，避免首月标签挤叠）
  const months = []
  let cm = new Date(t0)
  cm = new Date(cm.getFullYear(), cm.getMonth(), 1)
  const end = new Date(t1 + DAY)
  while (cm <= end) {
    const mx = x(cm.getTime())
    if (mx >= 8) months.push({ x: mx, label: (cm.getMonth() + 1) + '月' })
    cm = new Date(cm.getFullYear(), cm.getMonth() + 1, 1)
  }

  return {
    rows,
    above: rows.filter(r => r.above),
    below: rows.filter(r => !r.above),
    months,
    width: Math.round((t1 - t0) / DAY) * PX + PAD * 2,
    todayX: x(tToday),
    todayLabel: '当前日期 ' + todayStr.slice(5).replace('-', '/'),
    activeName: cur ? cur.name : null
  }
})

onMounted(async () => {
  try {
    const [ov, arts, unread] = await Promise.all([getInfoOverview(), getArticles(1, 20), getUnreadMessages()])
    overview.value = ov
    articles.value = arts.items
    unreadArticleIds.value = new Set(unread.articleIds || [])
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
.countdown {
  width: 100%;
  padding: 30px 20px;
  text-align: center;
}
.countdown .label { font-size: 14px; letter-spacing: 2px; }
.countdown .days {
  font-size: 72px;
  font-weight: 500;
  line-height: 1.15;
  margin: 4px 0;
}

.sec-title {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  margin: 26px 0 12px;
  color: var(--text);
}

/* ---- 时间轴 ---- */
.milestones { width: 100%; padding: 16px 0 12px; }
.tl-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0 18px;
  /* 滚动条只留灰色滑块：轨道透明、无箭头钮 */
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.35) transparent;
}
.tl-scroll::-webkit-scrollbar { height: 6px; }
.tl-scroll::-webkit-scrollbar-track { background: transparent; }
.tl-scroll::-webkit-scrollbar-button { display: none; height: 0; width: 0; }
.tl-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.32);
  border-radius: 3px;
}
.tl-scroll::-webkit-scrollbar-thumb:hover { background: rgba(255, 255, 255, 0.5); }
.tl-scroll::-webkit-scrollbar-corner { background: transparent; }
.tl {
  position: relative;
  min-width: 100%;
}

/* 当前日期徽标行 */
.tl-top { position: relative; height: 24px; }
.tl-today-badge {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  padding: 2px 9px;
  border-radius: 10px;
  background: var(--theme);
  color: var(--theme-on);
  font-size: 10px;
  letter-spacing: 1px;
  white-space: nowrap;
}

/* 标签行（上/下）：标签贴近轴线 */
.tl-row { position: relative; height: 46px; }
.tl-label {
  position: absolute;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
.tl-row.up .tl-label { bottom: 6px; }
.tl-row.down .tl-label { top: 6px; }
.tl-label b {
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.5px;
  color: var(--text-dim);
}
.tl-label i {
  font-style: normal;
  font-size: 9.5px;
  color: var(--text-faint);
}
.tl-label.active b { color: var(--theme-text); }
.tl-label.past { opacity: 0.55; }
/* 标签→轴 的短刻线 */
.tl-label::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 1px;
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
}
.tl-row.up .tl-label::after { bottom: -6px; }
.tl-row.down .tl-label::after { top: -6px; }

/* 轴 */
.tl-axis { position: relative; height: 18px; }
.tl-base {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 2px;
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.18);
}
.tl-seg {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.25);
}
.tl-seg.active {
  background: var(--theme);
  box-shadow: 0 0 7px rgb(var(--theme-rgb) / 0.5);
}
.tl-seg.past { opacity: 0.5; }
.tl-dot {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: inherit;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.12);
}
.tl-seg.active .tl-dot { background: var(--theme); }

/* 当前日期：虚线 + 菱形 */
.tl-today-line {
  position: absolute;
  top: -50px;
  height: 114px;
  border-left: 1.5px dashed rgb(var(--theme-rgb) / 0.75);
}
.tl-today-mark {
  position: absolute;
  top: 50%;
  width: 9px;
  height: 9px;
  background: var(--theme);
  transform: translate(-50%, -50%) rotate(45deg);
  box-shadow: 0 0 8px rgb(var(--theme-rgb) / 0.6);
}

/* 月份刻度 */
.tl-months { position: relative; height: 18px; margin-top: 2px; }
.tl-month {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--text-faint);
  white-space: nowrap;
}
.tl-month em {
  display: block;
  width: 1px;
  height: 4px;
  margin: 0 auto 1px;
  background: rgba(255, 255, 255, 0.18);
}

/* ---- 博客 ---- */
.articles { width: 100%; }
.article-card {
  width: 100%;
  padding: 17px 19px;
  margin-bottom: 11px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.article-card:hover { background: var(--glass-bg-strong); }
.a-main { flex: 1; min-width: 0; }
.a-title { font-size: 15.5px; font-weight: 500; line-height: 1.55; }
/* 未读回复红点（读者来信） */
.a-badge {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e5484d;
  margin-left: 8px;
  vertical-align: 2px;
}
.a-date { margin-top: 7px; font-size: 12px; }
.a-author { color: var(--theme-text); }
.a-cover {
  width: 78px; height: 58px;
  object-fit: cover;
  border-radius: 8px;
  flex: none;
}
.empty { text-align: center; padding: 26px 0; font-size: 14px; }

@media (min-width: 900px) {
  .tl-scroll { padding: 0 28px; }
}
</style>
