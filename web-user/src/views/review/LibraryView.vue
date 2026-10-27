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

    <!-- 沉浸刷题：悬浮球，对当前筛选的全部内容逐条播放（不占版面、不动复习状态） -->
    <button class="play-fab" @click="goPlay">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12a9 9 0 1 1-2.64-6.36" /><polyline points="21 3 21 9 15 9" />
      </svg>
      <span>沉浸刷题</span>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import TabRow from '@/components/TabRow.vue'
import EntryRow from '@/components/EntryRow.vue'
import { getSubjects, getEntries } from '@/api'
import { toast } from '@/utils/toast'

const router = useRouter()

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

// 沉浸刷题：把当前筛选（科目/类型）的全部内容逐条播放
function goPlay() {
  if (!total.value) return toast('这个筛选下还没有记录')
  const q = {}
  if (subjectId.value != null) q.subjectId = subjectId.value
  if (type.value != null) q.type = type.value
  router.push({ path: '/review/play', query: q })
}

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

/* 沉浸刷题悬浮球：fixed 悬浮，不影响下方内容 */
.play-fab {
  position: fixed;
  right: 22px;
  bottom: 26px;
  z-index: 60;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 999px;
  background: var(--glass-bg-strong);
  border: 1px solid var(--glass-border);
  color: var(--theme-text);
  font-size: 14px;
  letter-spacing: 2px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.22);
  cursor: pointer;
  transition: transform 0.15s, background 0.15s;
}
.play-fab:hover { background: rgb(var(--theme-rgb) / 0.22); transform: translateY(-2px); }
.play-fab svg { width: 18px; height: 18px; }
</style>
