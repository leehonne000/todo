<template>
  <div class="page page-narrow">
    <TopBar title="标签查询" />

    <!-- 标签选择区（width:100% 保证输入框只随屏幕变，不随内容抖） -->
    <div class="tag-panel glass">
      <input v-model.trim="filter" class="field" placeholder="输入标签后自动查询" />
      <div class="tag-list">
        <button
          v-for="t in filteredTags" :key="t.id"
          class="tag-item"
          :class="{ on: selectedIds.includes(t.id) }"
          @click="toggle(t.id)"
        >{{ t.name }}</button>
      </div>
      <div v-if="selectedIds.length" class="panel-foot">
        <button class="clear" @click="selectedIds = []">清空</button>
      </div>
    </div>

    <!-- 类别选择：note / 错题（统一 tab 风格） -->
    <TabRow v-model="type" :items="TABS" />

    <!-- 结果分区 -->
    <template v-if="searched">
      <section v-if="showNotes" class="result-sec">
        <EntryRow v-for="e in notes" :key="e.id" :entry="e" @click="$router.push(`/entry/${e.id}`)" />
        <p v-if="!notes.length" class="empty dim">没有对应的note</p>
      </section>

      <section v-if="showWrongs" class="result-sec">
        <EntryRow v-for="e in wrongs" :key="e.id" :entry="e" @click="$router.push(`/entry/${e.id}`)" />
        <p v-if="!wrongs.length" class="empty dim">没有对应的错题</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import TopBar from '@/components/TopBar.vue'
import TabRow from '@/components/TabRow.vue'
import EntryRow from '@/components/EntryRow.vue'
import { getTags, getEntries } from '@/api'
import { toast } from '@/utils/toast'

const TABS = [
  { value: null, label: '全部' },
  { value: 1, label: 'note' },
  { value: 2, label: '错题' }
]

const filter = ref('')
const allTags = ref([])
const selectedIds = ref([])
const results = ref([])
const searched = ref(false)
const type = ref(null) // null=全部 1=note 2=错题

const filteredTags = computed(() =>
  filter.value ? allTags.value.filter(t => t.name.includes(filter.value)) : allTags.value
)
const notes = computed(() => results.value.filter(e => e.type === 1))
const wrongs = computed(() => results.value.filter(e => e.type === 2))
const showNotes = computed(() => type.value !== 2)
const showWrongs = computed(() => type.value !== 1)

function toggle(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

let timer = null
watch(selectedIds, () => {
  clearTimeout(timer)
  timer = setTimeout(runSearch, 220)
}, { deep: true })

async function runSearch() {
  if (!selectedIds.value.length) {
    results.value = []
    searched.value = false
    return
  }
  try {
    const data = await getEntries({ tagIds: selectedIds.value.join(','), page: 1, size: 50 })
    results.value = data.items
    searched.value = true
  } catch (e) {
    toast(e.message)
  }
}

onMounted(async () => {
  try {
    allTags.value = await getTags()
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
.tag-panel { width: 100%; padding: 16px; margin-bottom: 14px; }
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 13px;
  min-height: 38px;
}
.tag-item {
  padding: 7px 15px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-dim);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid var(--glass-border);
}
.tag-item.on {
  color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.22);
  border-color: rgb(var(--theme-rgb) / 0.5);
}
.panel-foot {
  margin-top: 11px;
  display: flex;
  justify-content: flex-end;
}
.clear { color: var(--theme-text); font-size: 12px; }

.result-sec { width: 100%; margin-top: 16px; }
.empty {
  width: 100%;
  text-align: center;
  padding: 30px 0;
  font-size: 13.5px;
}
</style>
