<template>
  <div class="page">
    <div class="auth-box glass-strong">
      <h1 class="title">考研时间线</h1>
      <p class="sub">记录学过的，复习该忘的</p>

      <form @submit.prevent="onSubmit">
        <!-- readonly 直到聚焦：挡住浏览器自动填充旧账号（Chrome 会无视 autocomplete=off） -->
        <input v-model.trim="email" class="field" type="email" placeholder="邮箱"
               autocomplete="off" readonly @focus="unlockInput" />
        <input v-model="password" class="field" type="password" placeholder="密码"
               autocomplete="off" readonly @focus="unlockInput" />

        <button class="btn-amber submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <!-- 演示账号提示（mock 模式才显示） -->
      <div v-if="USE_MOCK" class="demo-hint">
        演示账号：<b>demo@kaoyan.com</b> / <b>demo123456</b>
        <button type="button" @click="fillDemo">一键填入</button>
      </div>

      <div class="links">
        <router-link to="/register">注册新账号</router-link>
        <router-link to="/reset">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '@/api'
import { USE_MOCK } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)

/** 聚焦即解锁输入框（readonly 只是进页面时挡自动填充用） */
function unlockInput(e) {
  e.target.removeAttribute('readonly')
}

function fillDemo() {
  email.value = 'demo@kaoyan.com'
  password.value = 'demo123456'
}

async function onSubmit() {
  if (!email.value || !password.value) return toast('邮箱和密码都要填哦')
  loading.value = true
  try {
    const data = await login(email.value, password.value)
    auth.setAuth(data)
    toast('欢迎回来')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { justify-content: center; }
.auth-box {
  width: 100%;
  max-width: 400px;
  padding: 40px 34px 30px;
}
.title { font-size: 26px; font-weight: 500; letter-spacing: 3px; text-align: center; }
.sub { margin: 10px 0 30px; text-align: center; color: var(--text-dim); font-size: 14px; letter-spacing: 1px; }
form { display: flex; flex-direction: column; gap: 14px; }
.submit { margin-top: 8px; width: 100%; padding: 14px; font-size: 16px; }
.links {
  margin-top: 22px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.links a { color: var(--text-dim); text-decoration: none; }
.links a:hover { color: var(--theme-text); }
.demo-hint {
  margin-top: 18px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgb(var(--theme-rgb) / 0.12);
  border: 1px dashed rgb(var(--theme-rgb) / 0.4);
  font-size: 12px;
  color: var(--text-dim);
  text-align: center;
  line-height: 1.8;
}
.demo-hint b { color: var(--theme-text); font-weight: 500; }
.demo-hint button {
  display: block;
  margin: 6px auto 0;
  color: var(--theme-text);
  font-size: 12px;
  text-decoration: underline;
}
</style>
