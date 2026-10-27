<template>
  <AdminShell>
    <div class="page-head">
      <div>
        <h1>硬节点管理</h1>
        <div class="sub">考研硬节点时间线数据（全局共享）——教育部公布后在此更新，倒计时取「初试」起始日</div>
      </div>
      <div class="actions">
        <button class="btn-amber" type="button" @click="openCreate">新建节点</button>
      </div>
    </div>

    <!-- 届数筛选：文字 + 主题色下划线（统一约定） -->
    <div class="filter-row">
      <TabRow v-model="yearFilter" :items="yearTabs" />
    </div>

    <div class="card glass">
      <div v-if="loading" class="empty">加载中…</div>
      <div v-else-if="!items.length" class="empty">这一届还没有硬节点，点右上角「新建节点」添加</div>
      <div v-else class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>届数</th>
              <th>节点名</th>
              <th>起始日</th>
              <th>结束日</th>
              <th>排序</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in items" :key="m.id">
              <td class="num">{{ m.examYear }}</td>
              <td>
                {{ m.name }}
                <span v-if="m.name === '初试'" class="badge badge-wrong" style="margin-left:8px;">倒计时基准</span>
              </td>
              <td class="num">{{ m.dateFrom }}</td>
              <td class="num dim">{{ m.dateTo || '—' }}</td>
              <td class="num dim">{{ m.sortOrder }}</td>
              <td>
                <div class="ops">
                  <button class="op-btn" type="button" @click="openEdit(m)">编辑</button>
                  <button class="op-btn danger" type="button" @click="askDelete(m)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新建/编辑表单 -->
    <Modal v-model="showForm" :title="editing ? '编辑硬节点' : '新建硬节点'"
           :confirm-text="editing ? '保存' : '创建'" :confirm-disabled="saving"
           @confirm="onSubmitForm">
      <div class="form-inline">
        <div class="form-row">
          <label>届数（如 2027）</label>
          <input v-model.number="form.examYear" class="field" type="number" min="2000" max="2100" />
        </div>
        <div class="form-row">
          <label>节点名（≤32 字）</label>
          <input v-model="form.name" class="field" type="text" maxlength="32" placeholder="如：预报名 / 初试" />
        </div>
      </div>
      <div class="form-inline">
        <div class="form-row">
          <label>起始日</label>
          <input v-model="form.dateFrom" class="field" type="date" />
        </div>
        <div class="form-row">
          <label>结束日（单日节点留空）</label>
          <input v-model="form.dateTo" class="field" type="date" />
        </div>
      </div>
      <div class="form-row">
        <label>排序（时间线展示顺序，升序）</label>
        <input v-model.number="form.sortOrder" class="field" type="number" min="0" />
      </div>
      <div class="cover-ops"><div class="hint">提示：name 填「初试」的节点驱动用户端倒计时，请勿随意改名。</div></div>
    </Modal>

    <Modal v-model="showDelete" title="删除硬节点" confirm-text="删除" @confirm="onDelete">
      <p style="line-height:1.8;font-size:14px;">
        确定删除「{{ pendingDelete?.examYear }} 届 · {{ pendingDelete?.name }}」吗？<br />
        <span class="dim">删除后用户端时间轴上不再显示该节点。</span>
      </p>
    </Modal>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminShell from '@/components/AdminShell.vue'
import TabRow from '@/components/TabRow.vue'
import Modal from '@/components/Modal.vue'
import {
  adminListMilestones, adminCreateMilestone, adminUpdateMilestone, adminDeleteMilestone
} from '@/api'
import { toast } from '@/utils/toast'

const ALL = 'all'
const yearFilter = ref(ALL)
const allItems = ref([])
const loading = ref(false)
const saving = ref(false)

const showForm = ref(false)
const editing = ref(null) // 编辑中的节点（null = 新建）
const form = ref({ examYear: new Date().getFullYear() + 1, name: '', dateFrom: '', dateTo: '', sortOrder: 0 })

const showDelete = ref(false)
const pendingDelete = ref(null)

// 届数 tab：从数据里聚合 + 全部
const yearTabs = computed(() => {
  const years = [...new Set(allItems.value.map(m => m.examYear))].sort((a, b) => a - b)
  return [{ value: ALL, label: '全部' }, ...years.map(y => ({ value: y, label: `${y} 届` }))]
})

const items = computed(() =>
  yearFilter.value === ALL ? allItems.value : allItems.value.filter(m => m.examYear === yearFilter.value)
)

async function load() {
  loading.value = true
  try {
    const data = await adminListMilestones()
    allItems.value = data.items
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = {
    examYear: yearFilter.value === ALL ? new Date().getFullYear() + 1 : yearFilter.value,
    name: '', dateFrom: '', dateTo: '', sortOrder: 0
  }
  showForm.value = true
}

function openEdit(m) {
  editing.value = m
  form.value = {
    examYear: m.examYear, name: m.name,
    dateFrom: m.dateFrom, dateTo: m.dateTo || '', sortOrder: m.sortOrder
  }
  showForm.value = true
}

function validate() {
  const f = form.value
  if (!f.examYear || f.examYear < 2000 || f.examYear > 2100) return '届数需在 2000~2100'
  if (!String(f.name || '').trim()) return '节点名必填'
  if (String(f.name).trim().length > 32) return '节点名不能超过 32 字'
  if (!f.dateFrom) return '起始日必填'
  if (f.dateTo && f.dateTo < f.dateFrom) return '结束日不能早于起始日'
  return null
}

async function onSubmitForm() {
  const err = validate()
  if (err) return toast(err)
  saving.value = true
  try {
    const payload = {
      examYear: Number(form.value.examYear),
      name: String(form.value.name).trim(),
      dateFrom: form.value.dateFrom,
      dateTo: form.value.dateTo || null,
      sortOrder: Number(form.value.sortOrder) >= 0 ? Number(form.value.sortOrder) : 0
    }
    if (editing.value) {
      await adminUpdateMilestone(editing.value.id, payload)
      toast('硬节点已更新')
    } else {
      await adminCreateMilestone(payload)
      toast('硬节点已创建')
    }
    showForm.value = false
    await load()
  } catch (e) {
    toast(e.message)
  } finally {
    saving.value = false
  }
}

function askDelete(m) {
  pendingDelete.value = m
  showDelete.value = true
}

async function onDelete() {
  const m = pendingDelete.value
  if (!m) return
  try {
    await adminDeleteMilestone(m.id)
    showDelete.value = false
    pendingDelete.value = null
    toast('硬节点已删除')
    await load()
  } catch (e) {
    toast(e.message)
  }
}

onMounted(load)
</script>
