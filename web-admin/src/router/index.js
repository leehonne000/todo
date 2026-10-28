import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/', redirect: '/articles' },
  { path: '/articles', name: 'articles', component: () => import('@/views/ArticlesView.vue') },
  { path: '/articles/new', name: 'article-new', component: () => import('@/views/ArticleEditView.vue') },
  { path: '/articles/:id/edit', name: 'article-edit', component: () => import('@/views/ArticleEditView.vue') },
  { path: '/users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { superOnly: true } },
  { path: '/milestones', name: 'milestones', component: () => import('@/views/MilestonesView.vue'), meta: { superOnly: true } },
  { path: '/messages', name: 'messages', component: () => import('@/views/MessagesView.vue') },
  { path: '/personal', name: 'personal', component: () => import('@/views/PersonalView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：管理后台需票据 + 管理员身份（后端每个 /api/admin/* 另有 2002 卡口兜底）
// superOnly 路由（用户任免/硬节点）仅超级管理员可进（2026-10-27 三级权限）
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.superOnly && !auth.isSuperAdmin) return { name: 'articles' }
  return true
})

export default router
