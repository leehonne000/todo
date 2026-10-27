// 登录态：token / 用户资料存 localStorage，刷新不丢
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
  // 角色三级制（2026-10-27）：0=普通用户 1=管理员 2=超级管理员（登录响应带回）——决定是否展示「管理后台」入口（§11.2 权限叠加）
  const role = ref(saved.role ?? null)
  // 资料（昵称/头像/届数）——登录后由 GET /api/profile 填充
  const profile = ref(saved.profile || null)

  const loggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => role.value === 1 || role.value === 2)
  const nickname = computed(() => profile.value?.nickname || (email.value ? email.value.split('@')[0] : ''))
  const avatarUrl = computed(() => profile.value?.avatarUrl || '/avatar-default.png')

  function setAuth({ token: t, userId: u, email: e, role: r }) {
    token.value = t
    userId.value = u
    email.value = e
    role.value = r ?? null
    persist()
  }

  function setProfile(p) {
    profile.value = p
    persist()
  }

  function persist() {
    localStorage.setItem(KEY, JSON.stringify({
      token: token.value, userId: userId.value, email: email.value, role: role.value, profile: profile.value
    }))
  }

  function logout() {
    token.value = ''
    userId.value = null
    email.value = ''
    role.value = null
    profile.value = null
    localStorage.removeItem(KEY)
  }

  return { token, userId, email, role, profile, loggedIn, isAdmin, nickname, avatarUrl, setAuth, setProfile, logout }
})
