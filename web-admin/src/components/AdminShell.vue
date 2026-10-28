<template>
  <!-- 管理后台骨架：左侧毛玻璃导航栏 + 右侧内容区（壁纸见 App.vue） -->
  <div class="shell" :class="{ collapsed, 'all-hidden': allHidden }">
    <aside class="side glass">
      <div class="brand">
        <img class="brand-logo" src="/logo.svg" alt="Big Tree" />
        <div class="brand-text">
          <div class="brand-name">Big Tree</div>
          <div class="brand-sub">管理后台</div>
        </div>
      </div>

      <nav class="nav">
        <router-link v-for="item in navItems" :key="item.to" :to="item.to" class="nav-item">
          <span class="nav-icon" v-html="item.icon"></span>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <!-- 左下角仅展示当前账号信息（入口已移到导航尾部「个性化」项，2026-10-27 用户改版） -->
        <div class="me">
          <img class="me-avatar" :src="avatarUrl" alt="头像" />
          <div class="who">
            <div class="who-name" v-if="nickname" :title="nickname">{{ nickname }}</div>
            <div class="who-email" :title="auth.email">{{ auth.email }}</div>
            <div class="who-role">{{ auth.isSuperAdmin ? '超级管理员' : '管理员' }}</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 全界面收起/展开键（2026-10-28 二次改版）：贴侧栏底部右缘（账号块上方），固定在导航栏上随其进退；
         点击收起导航+内容全部，只剩壁纸与本键（本键滑到整个界面最左侧），再点全部展开 -->
    <button class="all-toggle" :class="{ hidden: allHidden, 'nav-collapsed': collapsed }" type="button" @click="toggleAllHidden"
            :title="allHidden ? '展开整个界面' : '收起整个界面'">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6l-6 6 6 6" /><path d="M19 6l-6 6 6 6" /></svg>
    </button>

    <!-- 导航栏收起/展开键（2026-10-28 二次改版挪回侧栏右缘中央细长位）：只收导航栏，内容区铺满 -->
    <button class="side-toggle" :class="{ collapsed }" type="button" @click="toggleCollapsed"
            :title="collapsed ? '展开导航栏' : '收起导航栏'">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 6l-6 6 6 6" /></svg>
    </button>

    <main class="content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from '@/api'

const auth = useAuthStore()

// 左下角账号展示：头像 + 昵称 + 邮箱（2026-10-28 补昵称行；拉不到资料就用系统默认头像）
const avatarUrl = ref('/avatar-default.png')
const nickname = ref('')
async function loadProfile() {
  try {
    const p = await getProfile()
    if (p?.avatarUrl) avatarUrl.value = p.avatarUrl
    if (p?.nickname) nickname.value = p.nickname
  } catch { /* 静默回退默认头像 */ }
}
onMounted(() => {
  loadProfile()
  // 个性化页改完昵称/头像后即时刷新左下角（2026-10-28）
  window.addEventListener('profile-updated', loadProfile)
})
onUnmounted(() => window.removeEventListener('profile-updated', loadProfile))

// 侧栏收起/展开（2026-10-28）：收起时整个侧栏（品牌+导航+账号）退回界面外，
// 把手滑到整个界面最左侧，再点一下全部展开；偏好记 localStorage 刷新不丢
const collapsed = ref(localStorage.getItem('exam-timeline-side-collapsed') === '1')
function toggleCollapsed() {
  collapsed.value = !collapsed.value
  try { localStorage.setItem('exam-timeline-side-collapsed', collapsed.value ? '1' : '0') } catch { /* 忽略 */ }
}

// 全界面收起/展开（2026-10-28）：收起后导航+内容全部退场，只剩壁纸与主键（主键滑到界面最左）；
// 与上面的侧栏收起互不影响，各自记 localStorage 刷新不丢
const allHidden = ref(localStorage.getItem('exam-timeline-all-hidden') === '1')
function toggleAllHidden() {
  allHidden.value = !allHidden.value
  try { localStorage.setItem('exam-timeline-all-hidden', allHidden.value ? '1' : '0') } catch { /* 忽略 */ }
}

// 导航按角色裁剪（2026-10-27 三级权限）：用户任免与硬节点维护仅超管
const allNavItems = [
  {
    to: '/articles', label: '文章管理', superOnly: false,
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3h9l4 4v14H6z"/><path d="M15 3v4h4"/><path d="M9 12h7M9 16h7"/></svg>'
  },
  {
    to: '/users', label: '用户管理', superOnly: true,
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="9" cy="8" r="3.2"/><path d="M3.5 20c0-3.3 2.5-5.5 5.5-5.5s5.5 2.2 5.5 5.5"/><circle cx="17.5" cy="9.5" r="2.5"/><path d="M16 15c2.8 0 4.5 1.8 4.5 4.5"/></svg>'
  },
  {
    to: '/milestones', label: '硬节点管理', superOnly: true,
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2.5"/></svg>'
  }
]
// 「个性化」固定显示且排在该账号可见的全部管理项之后（2026-10-27 用户指定）
const personalItem = {
  to: '/personal', label: '个性化',
  icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.03 1.56V21a2 2 0 1 1-4 0v-.09A1.7 1.7 0 0 0 8.98 19.4a1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.7 1.7 0 0 0 .34-1.87 1.7 1.7 0 0 0-1.56-1.03H3a2 2 0 1 1 0-4h.09A1.7 1.7 0 0 0 4.6 8.98a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34H9a1.7 1.7 0 0 0 1.03-1.56V3a2 2 0 1 1 4 0v.09c0 .68.4 1.29 1.03 1.56a1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87V9c.27.63.88 1.03 1.56 1.03H21a2 2 0 1 1 0 4h-.09a1.7 1.7 0 0 0-1.56 1.03z"/></svg>'
}
const navItems = [...allNavItems.filter(i => !i.superOnly || auth.isSuperAdmin), personalItem]
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
}

/* ---------- 左侧导航 ---------- */
.side {
  position: fixed;
  top: 18px;
  bottom: 18px;
  left: 18px;
  width: 232px;
  display: flex;
  flex-direction: column;
  padding: 26px 18px 18px;
  z-index: 40;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 6px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.brand-logo { width: 44px; height: 44px; }
.brand-name { font-size: 19px; font-weight: 500; letter-spacing: 2px; }
.brand-sub { font-size: 11px; letter-spacing: 3px; color: var(--text-dim); margin-top: 2px; }

.nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 18px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  border-radius: 13px;
  color: var(--text-dim);
  text-decoration: none;
  font-size: 15px;
  letter-spacing: 1px;
  transition: background 0.15s, color 0.15s;
}
.nav-item:hover { background: rgba(255, 255, 255, 0.09); color: var(--text); }
.nav-item.router-link-active {
  background: rgb(var(--theme-rgb) / 0.18);
  color: var(--theme-text);
}
.nav-icon { display: flex; }
.nav-icon :deep(svg) { width: 19px; height: 19px; }

/* ---------- 底部：当前账号信息展示 ---------- */
.side-foot {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  padding-top: 14px;
}
.me {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 8px;
  border-radius: 13px;
}
.me-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
  flex: none;
}

.who { min-width: 0; text-align: left; }
.who-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.who-email {
  font-size: 11.5px;
  color: var(--text-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.who-email:first-child { font-size: 13px; color: var(--text); } /* 无昵称时邮箱升为主行 */
.who-role {
  margin-top: 3px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--theme-text);
}

/* ---------- 收起/展开把手（2026-10-28 二次改版）：中央细长键=导航栏，底部小键=全界面（固定在导航栏上随其进退） ---------- */
.side {
  transition: transform 0.3s ease, opacity 0.3s ease, visibility 0.3s;
}
.shell.collapsed .side {
  transform: translateX(calc(-100% - 30px));
  opacity: 0;
  pointer-events: none;
}
/* 全界面收起：导航+内容一起退场，只剩壁纸与全界面键 */
.shell.all-hidden .side,
.shell.all-hidden .content {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}

.all-toggle,
.side-toggle {
  position: fixed;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: var(--text);
  cursor: pointer;
  z-index: 50;
  transition: left 0.3s ease, background 0.15s, opacity 0.2s ease, visibility 0.2s;
}
.all-toggle:hover,
.side-toggle:hover { background: rgba(255, 255, 255, 0.24); }
.all-toggle svg,
.side-toggle svg { transition: transform 0.3s ease; }

/* 导航键：细长，压侧栏右缘垂直居中；导航收起后滑到整个界面最左侧 */
.side-toggle {
  top: 50%;
  transform: translateY(-50%);
  left: 241px; /* 侧栏右缘（18 + 232）减把手宽度一半，压在边上 */
  width: 18px;
  height: 128px;
}
.side-toggle svg { width: 13px; height: 13px; }
.side-toggle.collapsed { left: 6px; } /* 导航收起后贴界面最左侧 */
.side-toggle.collapsed svg { transform: rotate(180deg); }
.shell.all-hidden .side-toggle {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}

/* 全界面键：贴侧栏底部右缘（账号块上方），固定在导航栏上随其进退 */
.all-toggle {
  bottom: 110px;
  left: 241px;
  width: 18px;
  height: 56px;
}
.all-toggle svg { width: 12px; height: 12px; }
.all-toggle.nav-collapsed {
  left: -21px; /* 跟着导航栏一起退场（侧栏右缘已滑出屏幕） */
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}
/* 全界面收起后：键滑到整个界面最左侧，再点还原 */
.all-toggle.hidden {
  left: 6px;
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}
.all-toggle.hidden svg { transform: rotate(180deg); }

/* ---------- 右侧内容区 ---------- */
.content {
  flex: 1;
  min-width: 0;
  margin-left: 268px;
  padding: 30px 38px 60px 12px;
  transition: margin-left 0.3s ease, padding-left 0.3s ease, opacity 0.3s ease, visibility 0.3s;
}
.shell.collapsed .content {
  margin-left: 0;
  padding-left: 56px; /* 给最左侧的把手留位 */
}
</style>
