import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from './api'
import { USE_MOCK } from './api/client'
import { mockAdoptSession } from './api/mock'
import { writePref } from '@/utils/prefs'
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

  // 壁纸/主题随票据落地（2026-10-27 二次修复）：覆盖写两端存储，时间戳置为当下必赢仲裁，
  // 不依赖 cookie 共享——App 挂载前写好，store 初始化即读到跳转前的状态
  applyPrefs(identity.prefs)

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

/**
 * 落地票据携带的壁纸/主题偏好：覆盖写 cookie+localStorage，updatedAt 置为当下（必赢仲裁）。
 * 跳转前的现场状态直接随票据过来，不依赖两端 cookie 是否共享成功。
 */
function applyPrefs(prefs) {
  if (!prefs) return
  const now = Date.now()
  for (const [key, val] of [['exam-timeline-wallpaper', prefs.wallpaper], ['exam-timeline-theme', prefs.theme]]) {
    if (!val || typeof val !== 'object') continue
    try { writePref(key, JSON.stringify({ ...val, updatedAt: now })) } catch { /* 忽略 */ }
  }
}

consumeTicket().finally(() => {
  app.use(router)
  app.mount('#app')
})
