<template>
  <AdminShell>
    <div class="page-head">
      <div>
        <h1>文章管理</h1>
        <div class="sub">博客内容区的全部文章（含草稿）——发布后普通用户可见；每位管理员只能操作自己的文章</div>
      </div>
      <div class="actions">
        <button class="btn-amber" type="button" @click="$router.push('/articles/new')">写新文章</button>
      </div>
    </div>

    <!-- 状态筛选：文字 + 主题色下划线（统一约定） -->
    <div class="filter-row">
      <TabRow v-model="statusFilter" :items="statusTabs" />
    </div>

    <div class="card glass">
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!items.length" class="empty">这里空空的，点右上角「写新文章」开始</div>
      <div v-else class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>标题</th>
              <th>作者</th>
              <th>状态</th>
              <th>发布时间</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in items" :key="a.id">
              <td class="col-title" :title="a.title">{{ a.title }}</td>
              <td class="dim">{{ a.authorName }}<span v-if="a.authorId === auth.userId">（我）</span></td>
              <td>
                <span class="status" :class="a.status === 1 ? 'status-pub' : 'status-draft'">
                  {{ a.status === 1 ? '已发布' : '草稿' }}
                </span>
              </td>
              <td class="dim num">{{ fmtTime(a.publishedAt) }}</td>
              <td class="dim num">{{ fmtTime(a.updatedAt) }}</td>
              <td>
                <!-- 归属权（2026-10-27）：仅作者本人或超管可操作；其他管理员只读 -->
                <div v-if="canOperate(a)" class="ops">
                  <button class="op-btn" type="button" @click="$router.push(`/articles/${a.id}/edit`)">编辑</button>
                  <button v-if="a.status === 0" class="op-btn primary" type="button"
                          :disabled="busyId === a.id" @click="onPublish(a)">发布</button>
                  <button v-else class="op-btn" type="button"
                          :disabled="busyId === a.id" @click="onUnpublish(a)">下线</button>
                  <button class="op-btn danger" type="button"
                          :disabled="busyId === a.id" @click="askDelete(a)">删除</button>
                </div>
                <span v-else class="dim" style="font-size:12px;">仅作者可操作</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="total > size" class="pager">
        <span class="num">共 {{ total }} 篇</span>
        <button class="op-btn" type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="num">{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button class="op-btn" type="button" :disabled="page >= Math.ceil(total / size)" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>

    <Modal v-model="showDelete" title="删除文章"
           confirm-text="删除" @confirm="onDelete">
      <p style="line-height:1.8;font-size:14px;">
        确定删除《{{ pendingDelete?.title }}》吗？<br />
        <span class="dim">文章将被物理删除，封面图片一并清理，不可恢复。</span>
      </p>
    </Modal>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AdminShell from '@/components/AdminShell.vue'
import TabRow from '@/components/TabRow.vue'
import Modal from '@/components/Modal.vue'
import { adminListArticles, adminPublishArticle, adminUnpublishArticle, adminDeleteArticle } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const auth = useAuthStore()

// 归属权（2026-10-27）：作者本人或超管可操作
function canOperate(a) {
  return auth.isSuperAdmin || a.authorId === auth.userId
}

const statusTabs = [
  { value: 'all', label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' }
]
const statusFilter = ref('all')

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const busyId = ref(null)
const showDelete = ref(false)
const pendingDelete = ref(null)

const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))

function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (statusFilter.value !== 'all') params.status = statusFilter.value
    const data = await adminListArticles(params)
    items.value = data.items
    total.value = data.total
    // 翻页/筛选后若当前页超出范围，回到第一页
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

function goPage(p) {
  if (p < 1 || p > pages.value) return
  page.value = p
  load()
}

// 切换筛选回到第一页
watch(statusFilter, () => {
  page.value = 1
  load()
})

async function onPublish(a) {
  busyId.value = a.id
  try {
    await adminPublishArticle(a.id)
    toast('已发布，普通用户现在可见')
    await load()
  } catch (e) {
    toast(e.message)
  } finally {
    busyId.value = null
  }
}

async function onUnpublish(a) {
  busyId.value = a.id
  try {
    await adminUnpublishArticle(a.id)
    toast('已下线（文章保留为草稿，可再次发布）')
    await load()
  } catch (e) {
    toast(e.message)
  } finally {
    busyId.value = null
  }
}

function askDelete(a) {
  pendingDelete.value = a
  showDelete.value = true
}

async function onDelete() {
  const a = pendingDelete.value
  if (!a) return
  busyId.value = a.id
  try {
    await adminDeleteArticle(a.id)
    showDelete.value = false
    pendingDelete.value = null
    toast('文章已删除')
    await load()
  } catch (e) {
    toast(e.message)
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>
