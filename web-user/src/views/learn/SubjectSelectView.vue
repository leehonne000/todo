<template>
  <div class="page page-narrow">
    <TopBar title="选择科目" />
    <p class="tip dim">一次学习只选一门，本次记录全部归属它</p>

    <div class="subject-grid">
      <button
        v-for="s in subjects" :key="s.id"
        class="subject-card glass-strong"
        :disabled="starting"
        @click="onPick(s)"
      >
        <span class="s-name">{{ s.name }}</span>
        <span class="s-arrow dim">→</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { getSubjects, startSession } from '@/api'
import { useLearnStore } from '@/stores/learn'
import { toast } from '@/utils/toast'

const router = useRouter()
const learn = useLearnStore()
const subjects = ref([])
const starting = ref(false)

async function onPick(s) {
  if (starting.value) return
  starting.value = true
  try {
    // 契约 §5.2：选完科目进入计时那一刻建行（中断不丢）
    const { id, startedAt } = await startSession(s.id)
    learn.beginSession({ id, startedAt, subjectId: s.id, subjectName: s.name })
    router.push(`/learn/timer/${id}`)
  } catch (e) {
    toast(e.message)
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  try {
    subjects.value = await getSubjects()
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
.tip { text-align: center; font-size: 13px; margin: -6px 0 26px; }
.subject-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
@media (max-width: 420px) {
  .subject-grid { grid-template-columns: 1fr; }
}
.subject-card {
  padding: 26px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: transform 0.15s, background 0.15s;
}
.subject-card:hover { background: var(--glass-bg-strong); }
.subject-card:active { transform: scale(0.97); }
.s-name { font-size: 19px; font-weight: 500; letter-spacing: 2px; }
.s-arrow { font-size: 17px; }
</style>
