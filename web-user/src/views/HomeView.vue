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

    <!-- 签到卡：状态未回来前不渲染（已签到就从头到尾不出现，杜绝"闪一下再消失"）；
         点击后先播庆祝动画（今日已签 ✓），停留片刻再淡出 -->
    <Transition name="checkin-out">
      <button v-if="homeReady && !home.checkedInToday" class="checkin glass-strong"
              :class="{ celebrating: justChecked }" :disabled="justChecked" @click="onCheckIn">
        <svg v-if="!justChecked" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="3" /><path d="M16 2v4M8 2v4M3 10h18" />
        </svg>
        <svg v-else class="check-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path class="draw-check" d="M4 12.5l5 5 11-11" />
        </svg>
        <span class="c-label">{{ justChecked ? '今日已签' : '签到' }}</span>
        <span class="c-date num">{{ todayPretty }}</span>
      </button>
    </Transition>

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
const homeReady = ref(false) // /api/home 是否已返回：签到卡在此之前的渲染与否由它决定
const justChecked = ref(false) // 签到庆祝动画进行中
// 学习日口径：凌晨 4 点前仍算前一天
const todayPretty = prettyDate(studyDate())

async function load() {
  // 解耦：profile 拉取失败不能拖垮主界面状态（曾经 Promise.all 一挂全挂）
  const [h, p] = await Promise.allSettled([getHome(), getProfile()])
  if (h.status === 'fulfilled') {
    home.value = h.value
  } else {
    toast(h.reason?.message || '主界面状态加载失败')
  }
  homeReady.value = true // 无论成败都放开渲染门闩（失败时按"未签到"展示，允许用户尝试点签到）
  if (p.status === 'fulfilled') {
    auth.setProfile(p.value)
  } else {
    toast(p.reason?.message || '资料加载失败')
  }
}

async function onCheckIn() {
  if (home.value.checkedInToday || justChecked.value) return toast('今天已经签过到啦')
  try {
    await checkIn()
    // 先播庆祝动画，停留片刻再消失（交互反馈）
    justChecked.value = true
    setTimeout(() => {
      home.value.checkedInToday = true
    }, 1700)
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

/* 签到庆祝动画：弹跳 + 对勾描线，然后按钮淡出 */
.checkin.celebrating {
  animation: checkin-pop 0.55s ease;
  color: var(--theme-text);
}
.checkin.celebrating .check-svg { opacity: 1; }
.draw-check {
  stroke-dasharray: 30;
  stroke-dashoffset: 30;
  animation: checkin-draw 0.55s 0.15s ease forwards;
}
@keyframes checkin-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.07); }
  100% { transform: scale(1); }
}
@keyframes checkin-draw {
  to { stroke-dashoffset: 0; }
}
.checkin-out-leave-active { transition: opacity 0.45s ease, transform 0.45s ease; }
.checkin-out-leave-to { opacity: 0; transform: scale(0.9); }
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
