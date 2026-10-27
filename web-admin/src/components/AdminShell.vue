<template>
  <!-- 管理后台骨架：左侧毛玻璃导航栏 + 右侧内容区（壁纸见 App.vue） -->
  <div class="shell">
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
            <div class="who-email" :title="auth.email">{{ auth.email }}</div>
            <div class="who-role">{{ auth.isSuperAdmin ? '超级管理员' : '管理员' }}</div>
          </div>
        </div>
      </div>
    </aside>

    <main class="content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from '@/api'

const auth = useAuthStore()

// 左下角头像展示（拉不到资料就用系统默认头像）
const avatarUrl = ref('/avatar-default.png')
onMounted(async () => {
  try {
    const p = await getProfile()
    if (p?.avatarUrl) avatarUrl.value = p.avatarUrl
  } catch { /* 静默回退默认头像 */ }
})

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
.who-email {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.who-role {
  margin-top: 3px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--theme-text);
}

/* ---------- 右侧内容区 ---------- */
.content {
  flex: 1;
  min-width: 0;
  margin-left: 268px;
  padding: 30px 38px 60px 12px;
}
</style>
