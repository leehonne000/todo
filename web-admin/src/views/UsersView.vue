<template>
  <AdminShell>
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <div class="sub">按 id / 邮箱 / 昵称定位用户，任免管理员（超级管理员身份由部署者指定）</div>
      </div>
    </div>

    <div class="search-bar">
      <input v-model.trim="keyword" class="field" type="text"
             placeholder="搜索 id / 邮箱 / 昵称（留空看全部）"
             @keyup.enter="onSearch" />
      <button class="btn-glass" type="button" @click="onSearch">搜索</button>
    </div>

    <div class="card glass">
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!items.length" class="empty">没有匹配的用户</div>
      <div v-else class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>ID</th>
              <th>邮箱</th>
              <th>昵称</th>
              <th>角色</th>
              <th>考研届数</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in items" :key="u.id">
              <td class="num dim">{{ u.id }}</td>
              <td>{{ u.email }}</td>
              <td>{{ u.nickname || '—' }}</td>
              <td>
                <span class="status" :class="u.role >= 1 ? 'status-pub' : 'status-draft'">
                  {{ u.role === 2 ? '超级管理员' : (u.role === 1 ? '管理员' : '普通用户') }}
                </span>
              </td>
              <td class="num">{{ u.examYear }}</td>
              <td class="dim num">{{ fmtTime(u.createdAt) }}</td>
              <td>
                <div class="ops">
                  <!-- 三级权限（2026-10-27）：超管任免 0/1；超管身份与自己的角色都不可在此变更 -->
                  <template v-if="u.id !== auth.userId && u.role !== 2">
                    <button v-if="u.role === 0" class="op-btn primary" type="button"
                            :disabled="busyId === u.id" @click="onRole(u, 1)">提拔为管理员</button>
                    <button v-else class="op-btn danger" type="button"
                            :disabled="busyId === u.id" @click="askDemote(u)">取消管理员</button>
                  </template>
                  <span v-else class="dim" style="font-size:12px;">
                    {{ u.id === auth.userId ? '自己的角色不可自改' : '超管身份由部署者指定' }}
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="total > size" class="pager">
        <span class="num">共 {{ total }} 人</span>
        <button class="op-btn" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="num">{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="op-btn" type="button" :disabled="page >= Math.ceil(total / size)" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>

    <Modal v-model="showDemote" title="取消管理员"
           confirm-text="取消管理员" @confirm="onConfirmDemote">
      <p style="line-height:1.8;font-size:14px;">
        确定取消 <b>{{ pendingDemote?.email }}</b> 的管理员身份吗？<br />
        <span class="dim">取消后 TA 将无法进入管理后台，仍可正常使用用户端。</span>
      </p>
    </Modal>
  </AdminShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminShell from '@/components/AdminShell.vue'
import Modal from '@/components/Modal.vue'
import { adminListUsers, adminUpdateRole } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const auth = useAuthStore()

const keyword = ref('')
const appliedKeyword = ref('')
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const busyId = ref(null)
const showDemote = ref(false)
const pendingDemote = ref(null)

function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    const data = await adminListUsers({ keyword: appliedKeyword.value, page: page.value, size })
    items.value = data.items
    total.value = data.total
    if (!data.items.length && total.value > 0 && page.value > 1) {
      page.value = 1
      return load()
    }
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  appliedKeyword.value = keyword.value
  page.value = 1
  load()
}

function goPage(p) {
  if (p < 1 || p > Math.max(1, Math.ceil(total.value / size))) return
  page.value = p
  load()
}

async function onRole(u, role) {
  busyId.value = u.id
  try {
    await adminUpdateRole(u.id, role)
    toast(role === 1 ? `已把 ${u.email} 提拔为管理员` : `已取消 ${u.email} 的管理员身份`)
    showDemote.value = false
    pendingDemote.value = null
    await load()
  } catch (e) {
    toast(e.message)
  } finally {
    busyId.value = null
  }
}

function askDemote(u) {
  pendingDemote.value = u
  showDemote.value = true
}

function onConfirmDemote() {
  if (pendingDemote.value) onRole(pendingDemote.value, 0)
}

onMounted(load)
</script>
