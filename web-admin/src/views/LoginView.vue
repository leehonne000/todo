<template>
  <div class="page">
    <div class="auth-box glass-strong">
      <img class="brand-logo" src="/logo.svg" alt="Big Tree" />
      <h1 class="title">Big Tree</h1>
      <div class="sub">管理后台</div>

      <form @submit.prevent="onSubmit">
        <!-- readonly 直到聚焦：挡住浏览器自动填充旧账号 -->
        <input v-model.trim="email" class="field" type="email" placeholder="管理员邮箱"
               autocomplete="off" readonly @focus="unlockInput" />
        <input v-model="password" class="field" type="password" placeholder="密码"
               autocomplete="off" readonly @focus="unlockInput" />

        <button class="btn-amber submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <!-- 演示账号提示（mock 模式才显示） -->
      <div v-if="USE_MOCK" class="demo-hint">
        演示超管：<b>admin@bigtree.com</b>（全权，密码固定——「一键填入」自动带出，不显示明文）<br />
        演示管理员：<b>writer@bigtree.com</b> / <b>admin123456</b>（只动自己的文章）
        <button type="button" @click="fillDemo">一键填入超管</button>
      </div>

      <div class="links">
        <span class="faint">仅管理员可进入</span>
        <a href="/" @click.prevent="toUserSite">去用户端 →</a>
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
  email.value = 'admin@bigtree.com'
  // 固定超管密码只存 .env（不入库）；无 env 时回退 mock 演示密码
  password.value = import.meta.env?.VITE_SUPER_ADMIN_PASSWORD || 'admin123456'
}

function toUserSite() {
  // 用户端与管理后台分工程；开发期 5173，上线同域根路径
  location.href = import.meta.env.DEV ? 'http://localhost:5173/' : '/'
}

async function onSubmit() {
  if (!email.value || !password.value) return toast('邮箱和密码都要填哦')
  loading.value = true
  try {
    const data = await login(email.value, password.value)
    // 入口门禁：管理后台收管理员与超级管理员（后端 /api/admin/* 另有 2002 卡口兜底）
    if (data.role !== 1 && data.role !== 2) {
      toast('该账号不是管理员，无法进入管理后台')
      return
    }
    auth.setAuth(data)
    toast('欢迎回来，园丁')
    router.replace(route.query.redirect || '/articles')
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
.brand-logo { display: block; width: 88px; height: 88px; margin: 0 auto 14px; }
.title { font-size: 26px; font-weight: 500; letter-spacing: 3px; text-align: center; }
.sub {
  margin: 10px 0 30px;
  text-align: center;
  color: var(--text-dim);
  font-size: 14px;
  letter-spacing: 6px;
}
form { display: flex; flex-direction: column; gap: 14px; }
.submit { margin-top: 8px; width: 100%; padding: 14px; font-size: 16px; }
.links {
  margin-top: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
