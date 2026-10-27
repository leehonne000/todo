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

// —— 一次性全局数据重置（2026-10-25）——
// 演示数据在浏览器 localStorage 里，代码侧无法主动删；此处开闸清一次：
// 清空登录态/主题/壁纸偏好 + 旧版 mock 数据，演示数据由 mock v3 重新播种。
// 标记落位后本段自动失效，之后正常使用不再触发。
const RESET_MARK = 'exam-timeline-reset-20261026-3'
if (localStorage.getItem(RESET_MARK) !== '1') {
  for (const k of [
    'exam-timeline-auth',
    'exam-timeline-theme',
    'exam-timeline-wallpaper',
    'exam-timeline-mock-db',
    'exam-timeline-mock-db-v2',
    'exam-timeline-mock-db-v12',
    'exam-timeline-mock-db-v13',
    'exam-timeline-mock-db-v14'
  ]) localStorage.removeItem(k)
  localStorage.setItem(RESET_MARK, '1')
}

/**
 * 跨端票据登录（2026-10-27 方案 A，反向：管理后台「回到用户端」）：
 * 管理后台跳回用户端经 URL hash 带 #ticket=<身份JSON>。这里落地换身份后立即抹掉
 * hash，开门认的是跳转者本人，不再读本端残留的旧会话。与 web-admin/src/main.js 对称。
 * 真实模式拿票据换一次服务端校验（GET /api/profile 核验 token 与最新角色）并补全资料。
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
  if (USE_MOCK) mockAdoptSession(identity) // mock 鉴权靠会话表登记
  try {
    auth.setAuth({ token: identity.token, userId: null, email: '', role: null })
    const p = await getProfile()
    auth.setAuth({
      token: identity.token,
      userId: p.userId ?? identity.userId,
      email: p.email ?? identity.email,
      role: p.role ?? identity.role
    })
    auth.setProfile(p)
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

const app = createApp(App)
app.use(createPinia())
consumeTicket().finally(() => {
  app.use(router)
  app.mount('#app')
})

