<template>
  <div class="page page-narrow">
    <TopBar title="自定义" />

    <!-- 资料区：头像 + 昵称 + 届数（纯展示；修改入口=下方「修改昵称和头像」块，2026-10-28） -->
    <section class="profile">
      <div class="avatar-wrap">
        <img class="avatar" :src="auth.avatarUrl" alt="头像" />
      </div>
      <div class="nick-row">
        <div class="nick">{{ auth.nickname }}</div>
      </div>
      <div class="year-chip">{{ profileYear }} 届考研</div>
    </section>

    <!-- 折叠菜单 -->
    <section class="menu">
      <!-- 修改昵称和头像（2026-10-28：与编辑壁纸/主题色平级；顶部资料区改为纯展示） -->
      <div class="menu-block glass">
        <button class="menu-row" @click="toggle('profile')">
          <svg class="m-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="8" r="4" /><path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
          </svg>
          <span class="m-label">修改昵称和头像</span>
          <svg class="m-chev" :class="{ open: openSection === 'profile' }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 9l6 6 6-6" /></svg>
        </button>

        <transition name="slide">
          <div v-if="openSection === 'profile'" class="menu-body">
            <!-- 头像：点击更换（自动居中裁方 + 压缩后上传） -->
            <div class="prof-row">
              <button class="avatar-wrap sm" title="更换头像" @click="avatarInput?.click()">
                <img class="avatar" :src="auth.avatarUrl" alt="头像" />
                <span class="avatar-mask">更换</span>
              </button>
              <input ref="avatarInput" type="file" accept="image/*" hidden @change="onAvatarFile" />
              <div class="prof-tip dim">点击头像更换，自动裁方压缩后上传</div>
            </div>

            <!-- 昵称 -->
            <div class="prof-row">
              <input
                ref="nickInput" v-model.trim="nickDraft" class="nick-input" maxlength="20"
                placeholder="1~20 个字符" @keyup.enter="saveNickname"
              />
              <button class="mini-btn amber" @click="saveNickname">保存昵称</button>
            </div>
          </div>
        </transition>
      </div>

      <!-- 更换壁纸 -->
      <div class="menu-block glass">
        <button class="menu-row" @click="toggle('wallpaper')">
          <svg class="m-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="3" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
          </svg>
          <span class="m-label">编辑壁纸</span>
          <svg class="m-chev" :class="{ open: openSection === 'wallpaper' }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 9l6 6 6-6" /></svg>
        </button>

        <transition name="slide">
          <div v-if="openSection === 'wallpaper'" class="menu-body">
            <!-- 预览图内层随鼠标小幅视差，拖动滑杆时可直接感受动效强度 -->
            <div class="preview-img" @mousemove="onPreviewMove" @mouseleave="onPreviewLeave">
              <div
                class="preview-layer"
                :style="{ backgroundImage: `url(${wp.current})`, filter: wp.filterCss, transform: previewShift }"
              ></div>
              <div class="preview-tag">
                <span class="dim">{{ wp.category || '默认' }}</span>
                <span class="name">{{ wp.fileName }}</span>
              </div>
            </div>

            <!-- 画面调节：每根滑杆自带恢复默认 -->
            <div class="adjust">
              <div class="adj-head"><span>画面调节</span></div>
              <div class="adj-row">
                <span class="adj-label">亮度</span>
                <input class="adj-range" type="range" min="50" max="150" step="1" v-model.number="wp.brightness" />
                <span class="adj-val num">{{ wp.brightness }}%</span>
                <button class="adj-reset" @click="wp.brightness = 100">恢复默认</button>
              </div>
              <div class="adj-row">
                <span class="adj-label">对比度</span>
                <input class="adj-range" type="range" min="50" max="150" step="1" v-model.number="wp.contrast" />
                <span class="adj-val num">{{ wp.contrast }}%</span>
                <button class="adj-reset" @click="wp.contrast = 100">恢复默认</button>
              </div>
              <div class="adj-row">
                <span class="adj-label">饱和度</span>
                <input class="adj-range" type="range" min="0" max="200" step="1" v-model.number="wp.saturate" />
                <span class="adj-val num">{{ wp.saturate }}%</span>
                <button class="adj-reset" @click="wp.saturate = 100">恢复默认</button>
              </div>
              <!-- 动效强度：鼠标移动时壁纸视差位移幅度 -->
              <div class="adj-row">
                <span class="adj-label">动效强度</span>
                <input class="adj-range" type="range" min="0" max="100" step="1" v-model.number="wp.motion" />
                <span class="adj-val num">{{ wp.motion }}</span>
                <button class="adj-reset" @click="wp.resetMotion()">恢复默认</button>
              </div>
            </div>

            <button class="btn-amber roll-btn" :disabled="rolling" @click="panelOpen = !panelOpen">
              <span>{{ rolling ? '换景中…' : '换一张' }}</span>
              <svg class="chev" :class="{ open: panelOpen }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 9l6 6 6-6" /></svg>
            </button>

            <transition name="slide">
              <div v-if="panelOpen" class="sub-panel">
                <button class="sub-tag" :class="{ on: !wp.category }" :disabled="rolling" @click="pick('random')">
                  <span class="t-name">随机</span>
                  <span class="t-count dim">全库抽一张</span>
                </button>
                <button
                  v-for="c in categories" :key="c.name"
                  class="sub-tag" :class="{ on: wp.category === c.name }"
                  :disabled="rolling" @click="pick(c.name)"
                >
                  <span class="t-name">{{ c.name }}</span>
                  <span class="t-count dim">{{ c.files.length }} 张</span>
                </button>
              </div>
            </transition>

            <!-- 每日更新开关 -->
            <div class="switch-row">
              <span>每日更新</span>
              <button class="switch" :class="{ on: wp.dailyUpdate }" @click="wp.setDailyUpdate(!wp.dailyUpdate)">
                <i></i>
              </button>
            </div>
          </div>
        </transition>
      </div>

      <!-- 主题色 -->
      <div class="menu-block glass">
        <button class="menu-row" @click="toggle('theme')">
          <svg class="m-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="12" cy="12" r="9" /><circle cx="8.5" cy="10.5" r="1.2" fill="currentColor" /><circle cx="12" cy="7.5" r="1.2" fill="currentColor" /><circle cx="15.5" cy="10.5" r="1.2" fill="currentColor" />
          </svg>
          <span class="m-label">主题色</span>
          <svg class="m-chev" :class="{ open: openSection === 'theme' }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 9l6 6 6-6" /></svg>
        </button>

        <transition name="slide">
          <div v-if="openSection === 'theme'" class="menu-body">
            <div class="swatches">
              <!-- 恢复默认（标准白）：与其他色卡平级 -->
              <button class="swatch" :class="{ on: theme.main === '#ffffff' }" @click="theme.resetDefault()">
                <i class="dot" style="background: #ffffff"></i>
                <span class="s-name">恢复默认</span>
              </button>
              <button
                v-for="p in PRESETS" :key="p.name"
                class="swatch" :class="{ on: theme.main === p.main.toLowerCase() }"
                @click="theme.setTheme(p.main, p.deep)"
              >
                <i class="dot" :style="{ background: p.main }"></i>
                <span class="s-name">{{ p.name }}</span>
              </button>
              <!-- 色盘自定义 -->
              <label class="swatch" :class="{ on: theme.isCustom }" title="色盘自定义">
                <i class="dot rainbow">
                  <input type="color" :value="theme.main" @input="theme.setTheme($event.target.value)" />
                </i>
                <span class="s-name">自定义</span>
              </label>
            </div>
          </div>
        </transition>
      </div>

      <!-- 账号 -->
      <div class="menu-block glass">
        <button class="menu-row" @click="toggle('account')">
          <svg class="m-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="12" cy="8" r="4" /><path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
          </svg>
          <span class="m-label">账号</span>
          <svg class="m-chev" :class="{ open: openSection === 'account' }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 9l6 6 6-6" /></svg>
        </button>

        <transition name="slide">
          <div v-if="openSection === 'account'" class="menu-body">
            <div class="acc-row">
              <span class="dim">邮箱</span>
              <span>{{ auth.email }}</span>
            </div>
            <!-- 超级管理员专属：PC 管理后台入口（§11.2 权限叠加，一个账号走遍三界面） -->
            <button v-if="auth.isAdmin" class="btn-glass logout" @click="onAdmin">
              进入管理后台（PC）
            </button>
            <button class="btn-glass logout" @click="onLogout">退出登录</button>
          </div>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useWallpaperStore, loadManifest, randomFromCategory } from '@/stores/wallpaper'
import { useThemeStore, PRESETS } from '@/stores/theme'
import { getProfile, updateProfile, uploadImage } from '@/api'
import { toast } from '@/utils/toast'
import { readPref } from '@/utils/prefs'

const router = useRouter()
const auth = useAuthStore()
const wp = useWallpaperStore()
const theme = useThemeStore()

const categories = ref([])
const openSection = ref('') // '' = 全折叠；'profile' | 'wallpaper' | 'theme' | 'account'
const panelOpen = ref(false)
const rolling = ref(false)

// 昵称/头像修改（2026-10-28 挪进「修改昵称和头像」折叠块，顶部资料区纯展示）
const nickDraft = ref('')
const nickInput = ref(null)
const avatarInput = ref(null)

const profileYear = computed(() => auth.profile?.examYear || '—')

function toggle(name) {
  if (name === 'profile' && openSection.value !== 'profile') nickDraft.value = auth.nickname // 每次展开回填当前昵称
  openSection.value = openSection.value === name ? '' : name
}

// ---------- 壁纸 ----------
// 预览图鼠标视差：演示动效强度效果（幅度 = 动效强度 × 12px）
const pvx = ref(0)
const pvy = ref(0)
const previewShift = computed(() => {
  const amp = (wp.motion / 100) * 12
  return `translate3d(${(-pvx.value * amp).toFixed(1)}px, ${(-pvy.value * amp).toFixed(1)}px, 0)`
})
function onPreviewMove(e) {
  const r = e.currentTarget.getBoundingClientRect()
  pvx.value = ((e.clientX - r.left) / r.width) * 2 - 1
  pvy.value = ((e.clientY - r.top) / r.height) * 2 - 1
}
function onPreviewLeave() {
  pvx.value = 0
  pvy.value = 0
}

async function pick(name) {
  if (rolling.value) return
  rolling.value = true
  try {
    let cat = name
    if (name === 'random') {
      if (!categories.value.length) return toast('壁纸清单还没加载好')
      cat = categories.value[Math.floor(Math.random() * categories.value.length)].name
    }
    const picked = await randomFromCategory(cat)
    if (!picked) return toast('这个分类暂时没有壁纸')
    wp.setWallpaper(picked.src, { category: cat, fileName: picked.fileName })
  } finally {
    setTimeout(() => { rolling.value = false }, 650)
  }
}

// ---------- 昵称 ----------
async function saveNickname() {
  const n = nickDraft.value.trim()
  if (!n) return toast('昵称不能为空')
  if (n.length > 20) return toast('昵称最多 20 个字符')
  try {
    const data = await updateProfile({ nickname: n })
    auth.setProfile({ ...(auth.profile || {}), ...data })
    nickDraft.value = auth.nickname
    toast('昵称已更新')
  } catch (e) {
    toast(e.message)
  }
}

// ---------- 头像 ----------
function downscale(file, size = 256) {
  // 居中裁方 + 缩到 256，避免大图把本地存储塞爆
  return new Promise((resolve) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = canvas.height = size
      const ctx = canvas.getContext('2d')
      const s = Math.min(img.width, img.height)
      ctx.drawImage(img, (img.width - s) / 2, (img.height - s) / 2, s, s, 0, 0, size, size)
      canvas.toBlob(b => resolve(b || file), 'image/jpeg', 0.85)
      URL.revokeObjectURL(url)
    }
    img.onerror = () => resolve(file)
    img.src = url
  })
}

async function onAvatarFile(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  try {
    const small = await downscale(file)
    const up = await uploadImage(small)
    const data = await updateProfile({ avatarObjectKey: up.objectKey })
    auth.setProfile({ ...(auth.profile || {}), ...data })
    toast('头像已更换')
  } catch (err) {
    toast(err.message)
  }
}

function onAdmin() {
  // 方案 A（2026-10-27，修"同浏览器登两个账号进后台串号"）：票据带身份——
  // 跳后台时把当前登录票据（token+身份）经 URL hash 带过去（hash 不进服务器日志），
  // 后台落地即用"当前这个账号"换开门，不再读目标端残留的旧会话。
  const base = import.meta.env.DEV ? 'http://localhost:5174/' : '/admin/'
  const ticket = encodeURIComponent(JSON.stringify({
    token: auth.token, userId: auth.userId, email: auth.email, role: auth.role,
    // 壁纸/主题随票据带过去（2026-10-27 二次修复）：不依赖 cookie 共享，落地即覆盖，
    // 彻底消除"跳到另一端壁纸变了"（cookie 竞态 / localhost 与 127.0.0.1 不同域都会导致不同步）
    prefs: {
      wallpaper: tryParse(readPref('exam-timeline-wallpaper')),
      theme: tryParse(readPref('exam-timeline-theme'))
    }
  }))
  location.href = `${base}#ticket=${ticket}`
}

function tryParse(raw) {
  try { return JSON.parse(raw) } catch { return null }
}

function onLogout() {
  if (!confirm('确定退出登录？')) return
  auth.logout()
  router.replace('/login')
}

onMounted(async () => {
  categories.value = await loadManifest()
  try {
    const p = await getProfile()
    auth.setProfile(p)
  } catch { /* 资料拉不到不阻塞页面 */ }
})
</script>

<style scoped>
/* ---------- 资料区 ---------- */
.profile {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0 6px;
}
.avatar-wrap {
  position: relative;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.3);
}
.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.15s;
}
.avatar-wrap:hover .avatar-mask { opacity: 1; }

/* 「修改昵称和头像」折叠块（2026-10-28） */
.avatar-wrap.sm { width: 64px; height: 64px; flex: none; }
.prof-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.prof-row + .prof-row { margin-top: 12px; }
.prof-tip { font-size: 12.5px; }

.nick-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.nick {
  font-size: 21px;
  font-weight: 500;
  letter-spacing: 2px;
}
.nick:hover { color: var(--theme-text); }
.nick-input {
  width: 180px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid var(--glass-border);
  font-size: 15px;
  text-align: center;
  color: var(--text);
}
.mini-btn {
  padding: 7px 13px;
  border-radius: 999px;
  font-size: 12.5px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}
.mini-btn.amber { color: var(--theme-text); border-color: rgb(var(--theme-rgb) / 0.5); }

.year-chip {
  margin-top: 10px;
  padding: 5px 15px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid var(--glass-border);
  font-size: 12px;
  color: var(--text-dim);
  letter-spacing: 1px;
}

/* ---------- 折叠菜单 ---------- */
.menu {
  width: 100%;
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.menu-block { overflow: hidden; }
.menu-row {
  width: 100%;
  padding: 17px 18px;
  display: flex;
  align-items: center;
  gap: 13px;
}
.menu-row:hover { background: rgba(255, 255, 255, 0.06); }
.m-ic { width: 21px; height: 21px; color: var(--theme-text); flex: none; }
.m-label { font-size: 15.5px; letter-spacing: 1px; }
.m-chev {
  width: 18px;
  height: 18px;
  margin-left: auto;
  color: var(--text-faint);
  transition: transform 0.25s ease;
}
.m-chev.open { transform: rotate(180deg); }

.menu-body {
  padding: 4px 16px 18px;
}

/* ---------- 壁纸面板 ---------- */
.preview-img {
  height: clamp(150px, 24vw, 220px);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}
/* 预览图内层：承载背景图 + 动效视差位移（超出一圈防露边） */
.preview-layer {
  position: absolute;
  inset: -16px;
  background-size: cover;
  background-position: center;
  transition: transform 0.35s ease-out;
  will-change: transform;
}
.preview-tag {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  padding: 24px 13px 9px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11.5px;
  z-index: 1;
}
.preview-tag .name { font-size: 13px; }

/* 画面调节 */
.adjust {
  margin-top: 13px;
  padding: 11px 13px 5px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
}
.adj-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  letter-spacing: 1px;
  margin-bottom: 3px;
}
.adj-reset {
  width: 56px;
  flex: none;
  text-align: right;
  font-size: 11.5px;
  color: var(--theme-text);
}
.adj-reset:hover { text-decoration: underline; }
.adj-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 7px 0;
}
.adj-label { width: 58px; font-size: 12.5px; color: var(--text-dim); flex: none; white-space: nowrap; }
.adj-range {
  flex: 1;
  accent-color: var(--theme-text);
}
.adj-val {
  width: 42px;
  text-align: right;
  font-size: 11.5px;
  color: var(--text-faint);
  flex: none;
}

.roll-btn {
  width: 100%;
  margin-top: 11px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.roll-btn .chev { width: 16px; height: 16px; transition: transform 0.25s ease; }
.roll-btn .chev.open { transform: rotate(180deg); }

.sub-panel {
  margin-top: 3px;
  margin-left: 14px;
  padding: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-top: none;
  border-radius: 0 0 12px 12px;
}
.sub-tag {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--glass-border);
}
.sub-tag:hover { background: rgba(255, 255, 255, 0.14); }
.sub-tag.on {
  background: rgb(var(--theme-rgb) / 0.2);
  border-color: rgb(var(--theme-rgb) / 0.5);
}
.sub-tag.on .t-name { color: var(--theme-text); }
.t-name { font-size: 12.5px; }
.t-count { font-size: 10.5px; }

/* 每日更新开关 */
.switch-row {
  margin-top: 13px;
  padding: 12px 4px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}
.switch {
  width: 44px;
  height: 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid var(--glass-border);
  padding: 2px;
  transition: background 0.2s;
}
.switch i {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.switch.on { background: var(--theme-deep); }
.switch.on i { transform: translateX(20px); }

/* ---------- 主题色 ---------- */
.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  /* 色卡行整体居中：窗口变宽/换行时都不贴左 */
  justify-content: center;
}
.swatch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 11px 9px 9px;
  width: 78px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid var(--glass-border);
}
.swatch:hover { background: rgba(255, 255, 255, 0.13); }
.swatch.on {
  background: rgb(var(--theme-rgb) / 0.16);
  border-color: rgb(var(--theme-rgb) / 0.55);
}
.swatch .dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(128, 128, 128, 0.45);
}
.swatch.on .dot { border-color: #fff; }
.swatch .s-name { font-size: 11px; color: var(--text-dim); }
.swatch.on .s-name { color: var(--text); }
.rainbow {
  position: relative;
  background: conic-gradient(#f66, #fc6, #6c6, #6cf, #96f, #f6c, #f66);
  overflow: hidden;
}
.rainbow input[type='color'] {
  position: absolute;
  inset: -6px;
  width: 150%;
  height: 150%;
  opacity: 0;
  cursor: pointer;
}

/* ---------- 账号 ---------- */
.acc-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 2px;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logout { width: 100%; margin-top: 13px; padding: 12px; }

.slide-enter-active, .slide-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
