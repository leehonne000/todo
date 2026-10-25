<template>
  <div class="page home">
    <!-- 顶栏：头像（进自定义） + 右上角查询图标（图标不是搜索框，产品文档 §3） -->
    <header class="top">
      <button class="avatar" title="自定义" @click="$router.push('/settings')">
        <img :src="auth.avatarUrl" alt="头像" />
      </button>
      <button class="q-btn" title="标签查询" @click="$router.push('/query')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" />
        </svg>
      </button>
    </header>

    <!-- 签到卡 -->
    <button class="checkin glass-strong" :class="{ done: home.checkedInToday }" @click="onCheckIn">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="3" /><path d="M16 2v4M8 2v4M3 10h18" />
        <path v-if="home.checkedInToday" d="M9 15l2 2 4-4" stroke-width="2" />
      </svg>
      <span class="c-label">{{ home.checkedInToday ? '已签到' : '签到' }}</span>
      <span class="c-date num">{{ todayPretty }}</span>
    </button>

    <!-- Learn / Review 双卡（贴底） -->
    <div class="cards">
      <button class="card glass" @click="$router.push('/learn/subjects')">
        <div class="c-title">Learn</div>
        <div class="c-sub">
          <span v-if="home.todayLearned" class="check">✓ 今日已学习</span>
          <span v-else class="dim">今日学习</span>
        </div>
      </button>

      <button class="card glass" @click="onReview">
        <div class="c-title">Review</div>
        <div class="c-num num amber">{{ home.dueReviewCount }}</div>
      </button>
    </div>

    <DockNav />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHome, checkIn, getProfile } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { prettyDate, studyDate } from '@/utils/format'
import { toast } from '@/utils/toast'
import DockNav from '@/components/DockNav.vue'

const router = useRouter()
const auth = useAuthStore()

const home = ref({ checkedInToday: false, todayLearned: false, dueReviewCount: 0 })
// 学习日口径：凌晨 4 点前仍算前一天
const todayPretty = prettyDate(studyDate())

async function load() {
  try {
    const [h, p] = await Promise.all([getHome(), getProfile()])
    home.value = h
    auth.setProfile(p)
  } catch (e) {
    toast(e.message)
  }
}

async function onCheckIn() {
  if (home.value.checkedInToday) return toast('今天已经签过到啦')
  try {
    await checkIn()
    home.value.checkedInToday = true
    toast('签到成功，+1 天坚持')
  } catch (e) {
    toast(e.message)
  }
}

function onReview() {
  if (home.value.dueReviewCount <= 0) return toast('今天没有到期待复习，去学点新的吧')
  router.push('/review/due')
}

onMounted(load)
</script>

<style scoped>
.home {
  justify-content: flex-start;
  padding-bottom: 110px;
}

.top {
  width: 100%;
  max-width: var(--content-max);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  overflow: hidden;
  padding: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }

.q-btn {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text);
}
.q-btn:hover { background: rgba(255, 255, 255, 0.12); }
.q-btn svg { width: 20px; height: 20px; }

/* 签到卡 */
.checkin {
  margin: clamp(6vh, 9vh, 13vh) auto 0;
  width: 158px;
  height: 144px;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  transition: transform 0.15s ease;
}
.checkin:active { transform: scale(0.96); }
.checkin svg { width: 32px; height: 32px; opacity: 0.92; }
.checkin .c-label { font-size: 17px; font-weight: 500; letter-spacing: 2px; }
.checkin .c-date { font-size: 13px; color: var(--text-dim); letter-spacing: 0.5px; }
.checkin.done .c-label { color: var(--theme-text); }
.checkin.done svg { color: var(--theme-text); }

/* Learn / Review 卡：宽屏两个入口推远（space-between），窄窗自然收拢 */
.cards {
  margin-top: auto;
  width: 100%;
  max-width: var(--content-max);
  display: flex;
  justify-content: space-between;
  gap: var(--gap-cards);
}

.card {
  flex: 1 1 0;
  max-width: 420px;
  border-radius: 20px;
  padding: 18px 18px 16px;
  text-align: left;
  transition: transform 0.15s ease;
}
.card:active { transform: scale(0.97); }
.card .c-title { font-size: 18px; font-weight: 600; letter-spacing: 0.6px; }
.card .c-sub { margin-top: 8px; font-size: 13px; }
.card .c-sub .check { color: var(--theme-text); }
.card .c-num {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 500;
  line-height: 1.1;
}

/* 宽屏把签到卡也放大一号 */
@media (min-width: 900px) {
  .checkin { width: 184px; height: 168px; }
  .checkin svg { width: 38px; height: 38px; }
  .card { padding: 22px 24px 20px; }
  .card .c-title { font-size: 20px; }
  .card .c-num { font-size: 36px; }
}
</style>
