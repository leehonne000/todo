<template>
  <!-- 记录条目行卡片（查询结果 / 自由库共用） -->
  <article class="entry-row glass" role="button">
    <div class="row-top">
      <span class="badge" :class="entry.type === 1 ? 'badge-note' : 'badge-wrong'">
        {{ entry.type === 1 ? 'note' : '错题' }}
      </span>
      <span class="subject dim">{{ entry.subjectName }}</span>
      <span class="date dim num">{{ dateShort }}</span>
      <span v-if="entry.imageCount" class="img-count dim">图×{{ entry.imageCount }}</span>
    </div>
    <p class="content">{{ excerpt }}</p>
    <div v-if="entry.tags && entry.tags.length" class="tags">
      <span v-for="t in entry.tags" :key="t" class="mini-tag">#{{ t }}</span>
    </div>
    <div v-if="showRound" class="round dim">
      复习进度 <b class="amber num">{{ entry.round }}</b>/6 轮
      <span v-if="entry.reviewStatus === 1" class="done-mark">已出队</span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { dayOf } from '@/utils/format'

const props = defineProps({
  entry: { type: Object, required: true },
  showRound: { type: Boolean, default: true }
})

const excerpt = computed(() => {
  const c = props.entry.content || ''
  if (!c) return '（纯图片记录）'
  return c.length > 72 ? c.slice(0, 72) + '…' : c
})

const dateShort = computed(() => {
  // 归属学习日展示（凌晨 4 点前算前一天）
  return dayOf(props.entry.createdAt).slice(5).replace('-', '/')
})
</script>

<style scoped>
.entry-row {
  width: 100%;
  padding: 14px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background 0.15s, transform 0.12s;
  text-align: left;
}
.entry-row:hover { background: var(--glass-bg-strong); }
.entry-row:active { transform: scale(0.99); }

.row-top {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.subject { letter-spacing: 0.5px; }
.date { margin-left: auto; }
.img-count { margin-left: 8px; }

.content {
  margin-top: 9px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--text);
  opacity: 0.92;
}

.tags { margin-top: 9px; display: flex; flex-wrap: wrap; gap: 6px; }
.mini-tag {
  font-size: 11px;
  color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.12);
  padding: 2px 9px;
  border-radius: 999px;
}

.round { margin-top: 9px; font-size: 12px; }
.round b { font-weight: 500; }
.done-mark { margin-left: 8px; color: var(--text-faint); }
</style>
