<template>
  <div class="page">
    <div class="auth-box glass-strong">
      <h1 class="title">重置密码</h1>
      <p class="sub">通过注册邮箱验证身份</p>

      <form @submit.prevent="onSubmit">
        <div class="row">
          <input v-model.trim="email" class="field" type="email" placeholder="注册邮箱" />
          <button type="button" class="btn-glass code-btn" :disabled="countdown > 0" @click="onSendCode">
            {{ countdown > 0 ? `${countdown}s` : '发验证码' }}
          </button>
        </div>
        <input v-model.trim="code" class="field" maxlength="6" placeholder="6 位验证码" />
        <input v-model="newPassword" class="field" type="password" placeholder="新密码（6~32 位）" autocomplete="new-password" />

        <button class="btn-amber submit" type="submit" :disabled="loading">
          {{ loading ? '提交中…' : '重置密码' }}
        </button>
      </form>

      <div class="links">
        <router-link to="/login">想起来了，去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendCode, resetPassword } from '@/api'
import { toast } from '@/utils/toast'

const router = useRouter()
const email = ref('')
const code = ref('')
const newPassword = ref('')
const countdown = ref(0)
const loading = ref(false)
let timer = null

async function onSendCode() {
  if (!email.value) return toast('先填邮箱')
  try {
    const msg = await sendCode(email.value, 2) // purpose=2 找回密码
    toast(msg || '验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    toast(e.message)
  }
}

async function onSubmit() {
  if (!email.value || !code.value || !newPassword.value) return toast('三项都要填')
  if (newPassword.value.length < 6) return toast('新密码至少 6 位')
  loading.value = true
  try {
    await resetPassword({ email: email.value, code: code.value, newPassword: newPassword.value })
    toast('密码已重置，请重新登录')
    router.replace('/login')
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { justify-content: center; }
.auth-box { width: 100%; max-width: 400px; padding: 40px 34px 30px; }
.title { font-size: 24px; font-weight: 500; letter-spacing: 2px; text-align: center; }
.sub { margin: 10px 0 28px; text-align: center; color: var(--text-dim); font-size: 14px; }
form { display: flex; flex-direction: column; gap: 14px; }
.row { display: flex; gap: 10px; }
.row .field { flex: 1; }
.code-btn { flex: none; padding: 12px 16px; font-size: 13px; white-space: nowrap; }
.submit { margin-top: 8px; width: 100%; padding: 14px; font-size: 16px; }
.links { margin-top: 22px; text-align: center; font-size: 13px; }
.links a { color: var(--text-dim); text-decoration: none; }
.links a:hover { color: var(--theme-text); }
</style>
