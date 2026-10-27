import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 认证
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { public: true } },
  { path: '/reset', name: 'reset', component: () => import('@/views/ResetView.vue'), meta: { public: true } },

  // 主界面（根屏，常驻）
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },

  // 自定义（头像入口）
  { path: '/settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },

  // 标签查询（主界面右上角）
  { path: '/query', name: 'query', component: () => import('@/views/QueryView.vue') },

  // Learn 流程
  { path: '/learn/subjects', name: 'learn-subjects', component: () => import('@/views/learn/SubjectSelectView.vue') },
  { path: '/learn/timer/:sessionId', name: 'learn-timer', component: () => import('@/views/learn/TimerView.vue') },
  { path: '/learn/summary/:sessionId', name: 'learn-summary', component: () => import('@/views/learn/SummaryView.vue') },
  { path: '/learn/edit', name: 'learn-edit', component: () => import('@/views/learn/EntryEditView.vue') },

  // Review 沉浸式复习
  { path: '/review/due', name: 'review-due', component: () => import('@/views/review/DueView.vue') },

  // 三大模块
  { path: '/info', name: 'info', component: () => import('@/views/InfoView.vue') },
  { path: '/info/article/:id', name: 'article', component: () => import('@/views/ArticleView.vue') },
  { path: '/library', name: 'library', component: () => import('@/views/review/LibraryView.vue') },
  { path: '/entry/:id', name: 'entry', component: () => import('@/views/review/EntryDetailView.vue') },
  { path: '/progress', name: 'progress', component: () => import('@/views/ProgressView.vue') },
  // 学习趋势（统计页「连续签到」入口）：柱状/折线 × 本周/本月
  { path: '/progress/trend', name: 'progress-trend', component: () => import('@/views/ProgressTrendView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：未登录只能去认证页（后端没票据，所有业务接口都会拒）
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.token) return { name: 'login', query: { redirect: to.fullPath } }
  return true
})

export default router
