<template>
  <div class="page page-narrow">
    <TopBar title="复习 · 自由库" />
    <p class="tip dim">全部 note / 错题的自由浏览（艾宾浩斯队列只在主界面 Review 卡）</p>

    <!-- 科目筛选 + 类型筛选（统一 tab 风格，选一个） -->
    <div class="filter-rows">
      <TabRow :model-value="subjectId" :items="subjectTabs" @update:model-value="setSubject" />
      <TabRow :model-value="type" :items="TYPE_TABS" @update:model-value="setType" />
    </div>

    <div class="list">
      <EntryRow v-for="e in items" :key="e.id" :entry="e" @click="$router.push(`/entry/${e.id}`)" />
      <p v-if="!loading && !items.length" class="empty dim">这个筛选下还没有记录</p>
      <p v-if="loading" class="empty dim">加载中…</p>
    </div>

    <!-- 简单分页 -->
    <div v-if="total > size" class="pager">
      <button class="btn-glass" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span class="dim num">{{ page }} / {{ Math.ceil(total / size) }}</span>
      <button class="btn-glass" :disabled="page >= Math.ceil(total / size)" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import TabRow from '@/components/TabRow.vue'
import EntryRow from '@/components/EntryRow.vue'
import { getSubjects, getEntries } from '@/api'
import { toast } from '@/utils/toast'

const TYPE_TABS = [
  { value: null, label: '全部' },
  { value: 1, label: 'note' },
  { value: 2, label: '错题' }
]

const subjects = ref([])
const subjectTabs = computed(() => [
  { value: null, label: '全部' },
  ...subjects.value.map(s => ({ value: s.id, label: s.name }))
])
const items = ref([])
const subjectId = ref(null)
const type = ref(null)
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getEntries({
      subjectId: subjectId.value,
      type: type.value,
      page: page.value,
      size
    })
    items.value = data.items
    total.value = data.total
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

function setSubject(id) { subjectId.value = id; page.value = 1; load() }
function setType(t) { type.value = t; page.value = 1; load() }
function changePage(p) { page.value = p; load() }

onMounted(async () => {
  try {
    subjects.value = await getSubjects()
  } catch (e) {
    toast(e.message)
  }
  load()
})
</script>

<style scoped>
.tip { text-align: center; font-size: 12.5px; margin: -6px 0 18px; }
.filter-rows {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.list { width: 100%; margin-top: 16px; }
.empty { text-align: center; padding: 30px 0; font-size: 14px; }
.pager {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
}
.pager .btn-glass { padding: 8px 18px; font-size: 13px; }
</style>
