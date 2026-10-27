// 登录态：token / 账号 / 角色存 localStorage，刷新不丢
// 键名与 web-user 共用（部署同域时一个账号走遍三界面，§11.2 权限叠加）
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const KEY = 'exam-timeline-auth'

function load() {
  try { return JSON.parse(localStorage.getItem(KEY)) || {} } catch { return {} }
}

export const useAuthStore = defineStore('auth', () => {
  const saved = load()
  const token = ref(saved.token || '')
  const userId = ref(saved.userId || null)
  const email = ref(saved.email || '')
  // 角色三级制（2026-10-27）：0=普通用户 1=管理员 2=超级管理员（登录响应带回；管理后台入口门禁）
  const role = ref(saved.role ?? null)

  const loggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => role.value === 1 || role.value === 2)
  const isSuperAdmin = computed(() => role.value === 2)

  function setAuth({ token: t, userId: u, email: e, role: r }) {
    token.value = t
    userId.value = u
    email.value = e
    role.value = r ?? null
    persist()
  }

  function persist() {
    // 保留 web-user 写入的 profile 字段（同域共用一份登录态）
    const prev = load()
    localStorage.setItem(KEY, JSON.stringify({
      ...prev,
      token: token.value, userId: userId.value, email: email.value, role: role.value
    }))
  }

  function logout() {
    token.value = ''
    userId.value = null
    email.value = ''
    role.value = null
    localStorage.removeItem(KEY)
  }

  return { token, userId, email, role, loggedIn, isAdmin, isSuperAdmin, setAuth, logout }
})
