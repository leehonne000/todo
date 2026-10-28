<template>
  <!-- 个性化：与文章管理同级的常规页面（2026-10-27 用户改版：不做悬浮层，走侧栏导航项） -->
  <AdminShell>
    <div class="p-center">
    <div class="page-head">
      <div>
        <h1>个性化</h1>
        <div class="sub">壁纸、主题色与账号设置——改动即时生效，仅影响当前浏览器</div>
      </div>
    </div>

    <!-- 修改昵称和头像（2026-10-28：与编辑壁纸/主题色平级，用户点左下角头像改不了，入口收在这里） -->
    <section class="menu-block glass">
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
            <button class="avatar-wrap" title="更换头像" @click="avatarInput?.click()">
              <img class="avatar" :src="avatarUrl" alt="头像" />
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
            <button class="mini-btn amber" type="button" @click="saveNickname">保存昵称</button>
          </div>
        </div>
      </transition>
    </section>

    <!-- 编辑壁纸 -->
    <section class="menu-block glass">
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
    </section>

    <!-- 主题色 -->
    <section class="menu-block glass">
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
    </section>

    <!-- 账号：退出登录 -->
    <section class="menu-block glass">
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
          <div class="acc-row">
            <span class="dim">角色</span>
            <span>{{ auth.isSuperAdmin ? '超级管理员' : '管理员' }}</span>
          </div>
          <!-- 管理员一定是用户：回用户端走票票据带身份（与「进入管理后台」对称，防串号） -->
          <button class="btn-glass logout-btn" type="button" @click="backToUser">回到用户端</button>
          <button class="btn-glass logout-btn" type="button" @click="onLogout">退出登录</button>
        </div>
      </transition>
    </section>
    </div>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AdminShell from '@/components/AdminShell.vue'
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
const openSection = ref('') // 进来默认全部收起（2026-10-28 用户要求，不再默认展开壁纸区）；'profile'|'wallpaper'|'theme'|'account'
const panelOpen = ref(false) // 「换一张」分类面板
const rolling = ref(false)

// 昵称/头像修改（2026-10-28「修改昵称和头像」块）
const avatarUrl = ref('/avatar-default.png')
const nickDraft = ref('')
const nickInput = ref(null)
const avatarInput = ref(null)

onMounted(async () => {
  categories.value = await loadManifest()
  try {
    const p = await getProfile()
    if (p?.avatarUrl) avatarUrl.value = p.avatarUrl
    nickDraft.value = p?.nickname || auth.email.split('@')[0]
  } catch { /* 资料拉不到不阻塞页面 */ }
})

function toggle(name) {
  if (name === 'profile' && openSection.value !== 'profile') {
    // 每次展开回填当前昵称（避免沿用上次草稿）
    nickDraft.value = nickDraft.value || auth.email.split('@')[0]
  }
  openSection.value = openSection.value === name ? '' : name
}

// ---------- 昵称/头像 ----------
async function saveNickname() {
  const n = nickDraft.value.trim()
  if (!n) return toast('昵称不能为空')
  if (n.length > 20) return toast('昵称最多 20 个字符')
  try {
    const data = await updateProfile({ nickname: n })
    nickDraft.value = data?.nickname || n
    window.dispatchEvent(new CustomEvent('profile-updated', { detail: data })) // 通知 AdminShell 刷新左下角
    toast('昵称已更新')
  } catch (e) {
    toast(e.message)
  }
}

function downscale(file, size = 256) {
  // 居中裁方 + 缩到 256，避免大图把存储塞爆（与 web-user 同款）
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
    if (data?.avatarUrl) avatarUrl.value = data.avatarUrl
    window.dispatchEvent(new CustomEvent('profile-updated', { detail: data })) // 通知 AdminShell 刷新左下角
    toast('头像已更换')
  } catch (err) {
    toast(err.message)
  }
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

// ---------- 回到用户端（2026-10-27：管理员一定是用户）----------
// 与 web-user「进入管理后台」对称：票票据带身份过境，用户端落地即用"当前这个账号"恢复会话
function backToUser() {
  const base = import.meta.env.DEV ? 'http://localhost:5173/' : '/'
  const ticket = encodeURIComponent(JSON.stringify({
    token: auth.token, userId: auth.userId, email: auth.email, role: auth.role,
    // 壁纸/主题随票据带过去（2026-10-27 二次修复）：不依赖 cookie 共享，落地即覆盖，
    // 彻底消除"跳到另一端壁纸变了"
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

// ---------- 退出登录 ----------
function onLogout() {
  if (!confirm('确定退出登录？')) return
  auth.logout()
  toast('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
/* ---------- 整页居中（2026-10-27 用户指定） ---------- */
.p-center {
  max-width: 760px;
  margin: 0 auto;
}

/* ---------- 折叠菜单块（与 web-user 自定义页同款语言） ---------- */
.menu-block {
  border-radius: 16px;
  margin-bottom: 14px;
  overflow: hidden;
}
.menu-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: transparent;
  color: var(--text);
  font-size: 15px;
  letter-spacing: 1px;
  cursor: pointer;
}
.menu-row:hover { background: rgba(255, 255, 255, 0.07); }
.m-ic { width: 20px; height: 20px; color: var(--theme-text); flex: none; }
.m-label { flex: 1; text-align: left; }
.m-chev {
  width: 18px; height: 18px;
  color: var(--text-dim);
  transition: transform 0.22s ease;
}
.m-chev.open { transform: rotate(180deg); }

.menu-body {
  padding: 2px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 折叠过渡 */
.slide-enter-active, .slide-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-6px); }

/* ---------- 壁纸预览 ---------- */
.preview-img {
  position: relative;
  height: 180px;
  border-radius: 13px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
}
.preview-layer {
  position: absolute;
  inset: -16px;
  background-size: cover;
  background-position: center;
  transition: transform 0.3s ease-out;
  will-change: transform;
}
.preview-tag {
  position: absolute;
  left: 12px;
  bottom: 10px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(10, 12, 26, 0.55);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  font-size: 12px;
}
.preview-tag .name { color: var(--text); }

/* ---------- 画面调节 ---------- */
.adjust { display: flex; flex-direction: column; gap: 10px; }
.adj-head { font-size: 13px; letter-spacing: 2px; color: var(--text-dim); }
.adj-row {
  display: grid;
  grid-template-columns: 64px 1fr 52px 82px;
  align-items: center;
  gap: 12px;
}
.adj-label { font-size: 13px; color: var(--text-dim); }
.adj-range { width: 100%; accent-color: var(--theme-deep); cursor: pointer; }
.adj-val { font-size: 13px; text-align: right; color: var(--text); }
.adj-reset {
  padding: 5px 0;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-dim);
  font-size: 12px;
  cursor: pointer;
}
.adj-reset:hover { background: rgba(255, 255, 255, 0.18); color: var(--text); }

/* ---------- 换一张 + 分类 ---------- */
.roll-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.roll-btn .chev { width: 16px; height: 16px; transition: transform 0.22s ease; }
.roll-btn .chev.open { transform: rotate(180deg); }

.sub-panel { display: flex; flex-wrap: wrap; gap: 10px; }
.sub-tag {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 9px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.09);
  color: var(--text);
  cursor: pointer;
}
.sub-tag:hover { background: rgba(255, 255, 255, 0.16); }
.sub-tag.on {
  background: rgb(var(--theme-rgb) / 0.28);
  color: var(--theme-text);
}
.sub-tag:disabled { opacity: 0.45; cursor: not-allowed; }
.t-name { font-size: 14px; }
.t-count { font-size: 11px; }

/* ---------- 每日更新开关 ---------- */
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 0;
  font-size: 14px;
}
.switch {
  width: 46px;
  height: 25px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  padding: 3px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.switch i {
  display: block;
  width: 19px;
  height: 19px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s ease;
}
.switch.on { background: var(--theme-deep); }
.switch.on i { transform: translateX(21px); }

/* ---------- 主题色 ---------- */
.swatches {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 12px;
}
.swatch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 12px 6px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.07);
  color: var(--text);
  cursor: pointer;
}
.swatch:hover { background: rgba(255, 255, 255, 0.14); }
.swatch.on {
  background: rgb(var(--theme-rgb) / 0.24);
  color: var(--theme-text);
}
.swatch .dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
}
.swatch .dot.rainbow {
  position: relative;
  overflow: hidden;
  background: conic-gradient(#f66, #fc6, #6c6, #6cf, #c6f, #f66);
  display: flex;
}
.swatch .dot.rainbow input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.swatch .s-name { font-size: 12px; letter-spacing: 1px; }

/* ---------- 账号 / 退出 ---------- */
.acc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 2px 2px 0;
  font-size: 14px;
}
.logout-btn {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  letter-spacing: 2px;
}

/* ---------- 修改昵称和头像（2026-10-28） ---------- */
.prof-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.prof-row + .prof-row { margin-top: 14px; }
.prof-tip { font-size: 12.5px; color: var(--text-dim); }
.avatar-wrap {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.3);
  padding: 0;
  flex: none;
  cursor: pointer;
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
.nick-input {
  width: 220px;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid var(--glass-border);
  font-size: 14px;
  color: var(--text);
}
.mini-btn {
  padding: 9px 16px;
  border-radius: 999px;
  font-size: 13px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  color: var(--text);
  cursor: pointer;
}
.mini-btn.amber {
  color: var(--theme-text);
  border-color: rgb(var(--theme-rgb) / 0.5);
}
</style>
