<template>
  <div class="page">
    <div class="auth-box glass-strong">
      <h1 class="title">创建账号</h1>
      <p class="sub">注册时定下届数与科目，之后不再更改</p>

      <form @submit.prevent="onSubmit">
        <!-- 邮箱 + 验证码 -->
        <div class="row">
          <input v-model.trim="email" class="field" type="email" placeholder="邮箱"
                 autocomplete="off" readonly @focus="unlockInput" />
          <button type="button" class="btn-glass code-btn" :disabled="countdown > 0" @click="onSendCode">
            {{ countdown > 0 ? `${countdown}s` : '发验证码' }}
          </button>
        </div>
        <input v-model.trim="code" class="field" maxlength="6" placeholder="6 位验证码" />
        <input v-model="password" class="field" type="password" placeholder="密码（6~32 位）"
                 autocomplete="off" readonly @focus="unlockInput" />
        <input v-model.number="examYear" class="field" type="number" placeholder="考研届数，如 2027" />

        <!-- 科目组合：2~5 门 -->
        <div class="label">科目组合（选 2~5 门）</div>
        <div class="subjects">
          <button
            v-for="s in SUBJECT_PRESETS" :key="s"
            type="button"
            class="sub-chip"
            :class="{ on: subjects.includes(s) }"
            @click="toggleSubject(s)"
          >{{ s }}</button>
          <button type="button" class="sub-chip add" @click="customOpen = !customOpen">＋自定义</button>
        </div>
        <div v-if="customOpen" class="row">
          <input v-model.trim="customName" class="field" placeholder="自定义科目名" @keyup.enter="addCustom" />
          <button type="button" class="btn-glass code-btn" @click="addCustom">添加</button>
        </div>

        <button class="btn-amber submit" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注册并登录' }}
        </button>
      </form>

      <div class="links">
        <router-link to="/login">已有账号，去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendCode, register } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const SUBJECT_PRESETS = ['政治', '英语一', '英语二', '数学一', '数学二', '数学三', '408', '专业课']

const router = useRouter()
const auth = useAuthStore()


/** 聚焦即解锁输入框（readonly 只是进页面时挡浏览器自动填充用） */
function unlockInput(e) {
  e.target.removeAttribute('readonly')
}

const email = ref('')
const code = ref('')
const password = ref('')
const examYear = ref(new Date().getFullYear() + 1)
const subjects = ref(['政治', '英语一', '数学三'])
const customOpen = ref(false)
const customName = ref('')
const countdown = ref(0)
const loading = ref(false)
let timer = null

function toggleSubject(s) {
  const i = subjects.value.indexOf(s)
  if (i >= 0) subjects.value.splice(i, 1)
  else if (subjects.value.length >= 5) toast('最多 5 门')
  else subjects.value.push(s)
}

function addCustom() {
  const name = customName.value.trim()
  if (!name) return
  if (subjects.value.includes(name)) return toast('已在列表中')
  if (subjects.value.length >= 5) return toast('最多 5 门')
  subjects.value.push(name)
  customName.value = ''
  customOpen.value = false
}

async function onSendCode() {
  if (!email.value) return toast('先填邮箱')
  try {
    const msg = await sendCode(email.value, 1) // purpose=1 注册激活
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
  if (!email.value || !code.value || !password.value) return toast('邮箱、验证码、密码都要填')
  if (password.value.length < 6) return toast('密码至少 6 位')
  if (subjects.value.length < 2) return toast('科目至少选 2 门')
  loading.value = true
  try {
    const data = await register({
      email: email.value,
      password: password.value,
      examYear: examYear.value,
      code: code.value,
      subjects: [...subjects.value]
    })
    auth.setAuth(data)
    toast('注册成功，开始你的考研时间线')
    router.replace('/')
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { justify-content: center; }
.auth-box { width: 100%; max-width: 440px; padding: 36px 30px 26px; }
.title { font-size: 24px; font-weight: 500; letter-spacing: 2px; text-align: center; }
.sub { margin: 8px 0 26px; text-align: center; color: var(--text-dim); font-size: 13px; }
form { display: flex; flex-direction: column; gap: 13px; }
.row { display: flex; gap: 10px; }
.row .field { flex: 1; }
.code-btn { flex: none; padding: 12px 16px; font-size: 13px; white-space: nowrap; }
.label { font-size: 13px; color: var(--text-dim); margin-top: 4px; }
.subjects { display: flex; flex-wrap: wrap; gap: 9px; }
.sub-chip {
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-dim);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid var(--glass-border);
  transition: all 0.15s;
}
.sub-chip.on {
  color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.2);
  border-color: rgb(var(--theme-rgb) / 0.5);
}
.sub-chip.add { border-style: dashed; }
.submit { margin-top: 10px; width: 100%; padding: 14px; font-size: 16px; }
.links { margin-top: 18px; text-align: center; font-size: 13px; }
.links a { color: var(--text-dim); text-decoration: none; }
.links a:hover { color: var(--theme-text); }
</style>
