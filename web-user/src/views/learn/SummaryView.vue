<template>
  <div class="page page-narrow">
    <TopBar title="学习总结">
      <span class="subject-pill">{{ learn.subjectName }}</span>
    </TopBar>

    <!-- 三件套：添加note / 添加错题 / Done（产品文档 §4.3 极简） -->
    <button class="add-btn glass-strong" @click="goEdit(1)">
      <span>添加 note</span>
      <span class="plus">＋</span>
    </button>
    <!-- note 子条目：贴在按钮正下方，像子标签 -->
    <div class="kids">
      <button
        v-for="(d, i) in noteDrafts" :key="'n' + i"
        class="kid" @click="goEdit(d.type, d.index)"
      >
        <span class="badge badge-note">note</span>
        <span class="kid-text">{{ draftText(d) }}</span>
        <span class="chev">›</span>
      </button>
    </div>

    <button class="add-btn glass-strong" @click="goEdit(2)">
      <span>添加错题</span>
      <span class="plus">＋</span>
    </button>
    <div class="kids">
      <button
        v-for="(d, i) in wrongDrafts" :key="'w' + i"
        class="kid" @click="goEdit(d.type, d.index)"
      >
        <span class="badge badge-wrong">错题</span>
        <span class="kid-text">{{ draftText(d) }}</span>
        <span class="chev">›</span>
      </button>
    </div>

    <!-- Done 胶囊：右下角琥珀色 -->
    <button class="btn-amber done-btn" :disabled="saving" @click="onDone">
      {{ saving ? '保存中…' : 'Done' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { useLearnStore } from '@/stores/learn'
import { saveSessionEntries } from '@/api'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const learn = useLearnStore()
const saving = ref(false)

const noteDrafts = computed(() =>
  learn.drafts.map((d, index) => ({ ...d, index })).filter(d => d.type === 1)
)
const wrongDrafts = computed(() =>
  learn.drafts.map((d, index) => ({ ...d, index })).filter(d => d.type === 2)
)

function draftText(d) {
  if (d.content) return d.content.length > 26 ? d.content.slice(0, 26) + '…' : d.content
  return `纯图片记录（${d.imageObjectKeys.length} 张）`
}

function goEdit(type, index = -1) {
  learn.editIndex = index
  router.push({ path: '/learn/edit', query: { type, index } })
}

async function onDone() {
  if (saving.value) return
  // 空手而归也算完成（用户可能只计时不记录）
  saving.value = true
  try {
    const entries = learn.drafts.map(d => ({
      type: d.type,
      content: d.content,
      tagNames: d.tagNames,
      imageObjectKeys: d.imageObjectKeys
    }))
    await saveSessionEntries(route.params.sessionId, entries)
    toast(entries.length ? `已保存 ${entries.length} 条记录` : '学习已记录')
    learn.reset()
    router.replace('/')
  } catch (e) {
    toast(e.message)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.subject-pill {
  margin-left: auto;
  font-size: 12px;
  color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.16);
  padding: 5px 13px;
  border-radius: 999px;
}

.add-btn {
  width: 100%;
  padding: 19px 22px;
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 17px;
  letter-spacing: 1px;
  transition: background 0.15s, transform 0.12s;
}
.add-btn:hover { background: var(--glass-bg-strong); }
.add-btn:active { transform: scale(0.98); }
.add-btn .plus { font-size: 21px; color: var(--theme-text); }

/* 子条目：比按钮小一点、左缩进、贴着按钮下方（margin-top: 3px） */
.kids { display: flex; flex-direction: column; }
.kid {
  margin-top: 3px;
  margin-left: 14px;
  padding: 9px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-top: none;
  border-radius: 0 0 12px 12px;
  font-size: 13px;
  transition: background 0.15s;
}
.kid:hover { background: rgba(255, 255, 255, 0.16); }
.kid-text {
  flex: 1;
  text-align: left;
  color: var(--text-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chev { color: var(--text-faint); font-size: 17px; }

.done-btn {
  position: fixed;
  bottom: 28px;
  /* 跟随内容栏右缘：手机（100vw ≤ 内容宽）→ 贴右下角 26px；
     屏幕变宽 → 随内容栏右缘向中间移，但始终保持右侧对齐（不居中） */
  right: calc(max(0px, (100vw - var(--content-max)) / 2) + var(--pad-x) + 8px);
  padding: 13px 36px;
  font-size: 16px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.3);
}
</style>
