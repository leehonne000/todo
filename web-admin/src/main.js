import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from './api'
import { USE_MOCK } from './api/client'
import { mockAdoptSession } from './api/mock'
import './styles/base.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

/**
 * 跨端票据登录（2026-10-27 方案 A）：用户端「进入管理后台」经 URL hash 带
 * #ticket=<身份JSON>（token/userId/email/role）。这里落地换身份后立即抹掉 hash，
 * 修"同浏览器登两个账号时进后台串号"——开门认的是跳转者，不再是目标端残留的旧会话。
 * 真实模式再拿票据换一次服务端校验（GET /api/profile 核验 token 有效性与最新角色）。
 */
async function consumeTicket() {
  const m = (location.hash || '').match(/ticket=([^&]+)/)
  if (!m) return
  let identity = null
  try { identity = JSON.parse(decodeURIComponent(m[1])) } catch { /* 坏票据当没有 */ }
  history.replaceState(null, '', location.pathname + location.search) // 抹掉票据痕迹
  if (!identity?.token) return

  const auth = useAuthStore()
  if (USE_MOCK) {
    // mock 鉴权靠会话表登记：把票据身份登记进去即可通行
    mockAdoptSession(identity)
    auth.setAuth(identity)
    return
  }
  try {
    auth.setAuth({ token: identity.token, userId: null, email: '', role: null })
    const p = await getProfile()
    auth.setAuth({
      token: identity.token,
      userId: p.userId ?? identity.userId,
      email: p.email ?? identity.email,
      role: p.role ?? identity.role
    })
  } catch {
    auth.logout()
    location.href = '/login'
  }
}

consumeTicket().finally(() => {
  app.use(router)
  app.mount('#app')
})
