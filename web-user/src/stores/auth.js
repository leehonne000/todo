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
  // 资料（昵称/头像/届数）——登录后由 GET /api/profile 填充
  const profile = ref(saved.profile || null)

  const loggedIn = computed(() => Boolean(token.value))
  const nickname = computed(() => profile.value?.nickname || (email.value ? email.value.split('@')[0] : ''))
  const avatarUrl = computed(() => profile.value?.avatarUrl || '/avatar-default.png')

  function setAuth({ token: t, userId: u, email: e }) {
    token.value = t
    userId.value = u
    email.value = e
    persist()
  }

  function setProfile(p) {
    profile.value = p
    persist()
  }

  function persist() {
    localStorage.setItem(KEY, JSON.stringify({
      token: token.value, userId: userId.value, email: email.value, profile: profile.value
    }))
  }

  function logout() {
    token.value = ''
    userId.value = null
    email.value = ''
    profile.value = null
    localStorage.removeItem(KEY)
  }

  return { token, userId, email, profile, loggedIn, nickname, avatarUrl, setAuth, setProfile, logout }
})
