<template>
  <div class="page page-narrow">
    <TopBar title="信息" />

    <!-- 固定信息区：倒计时 -->
    <section class="countdown glass-strong">
      <p class="label dim">距离 {{ overview.examYear }} 年考研还有</p>
      <p class="days num amber">{{ overview.daysLeft }}</p>
      <p class="label dim">天</p>
    </section>

    <!-- 考研硬节点时间线 -->
    <section class="milestones glass">
      <h2 class="sec-title">考研硬节点</h2>
      <ul class="timeline">
        <li v-for="m in overview.milestones" :key="m.name" class="ms" :class="{ next: isNext(m) }">
          <span class="dot"></span>
          <span class="ms-name">{{ m.name }}</span>
          <span class="ms-date dim num">{{ fmtRange(m) }}</span>
        </li>
      </ul>
    </section>

    <!-- 博客内容区（超级管理员发布，用户只读） -->
    <section class="articles">
      <h2 class="sec-title">博客</h2>
      <article
        v-for="a in articles" :key="a.id"
        class="article-card glass" @click="$router.push(`/info/article/${a.id}`)"
      >
        <div class="a-main">
          <h3 class="a-title">{{ a.title }}</h3>
          <p class="a-date dim num">{{ (a.publishedAt || '').replace('T', ' ').slice(0, 10) }}</p>
        </div>
        <img v-if="a.coverUrl" class="a-cover" :src="a.coverUrl" alt="" />
      </article>
      <p v-if="!articles.length" class="empty dim">还没有发布文章</p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import { getInfoOverview, getArticles } from '@/api'
import { daysFromToday } from '@/utils/format'
import { toast } from '@/utils/toast'

const overview = ref({ examYear: '', daysLeft: 0, milestones: [] })
const articles = ref([])

function fmtRange(m) {
  const f = m.dateFrom.slice(5).replace('-', '/')
  const t = m.dateTo ? m.dateTo.slice(5).replace('-', '/') : ''
  return t && t !== f ? `${f} – ${t}` : f
}

function isNext(m) {
  // 刚过或未来的第一个节点高亮
  const d = daysFromToday(m.dateFrom)
  return d >= -3
}

onMounted(async () => {
  try {
    const [ov, arts] = await Promise.all([getInfoOverview(), getArticles(1, 20)])
    overview.value = ov
    articles.value = arts.items
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

.milestones { width: 100%; padding: 18px 22px; }
.timeline { list-style: none; }
.ms {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 14px;
}
.ms:last-child { border-bottom: none; }
.ms .dot {
  width: 9px; height: 9px; border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  flex: none;
}
.ms.next .dot { background: var(--theme-text); box-shadow: 0 0 8px rgb(var(--theme-rgb) / 0.6); }
.ms.next .ms-name { color: var(--theme-text); }
.ms-name { letter-spacing: 1px; }
.ms-date { margin-left: auto; font-size: 12.5px; }

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
.a-date { margin-top: 7px; font-size: 12px; }
.a-cover {
  width: 78px; height: 58px;
  object-fit: cover;
  border-radius: 8px;
  flex: none;
}
.empty { text-align: center; padding: 26px 0; font-size: 14px; }
</style>
