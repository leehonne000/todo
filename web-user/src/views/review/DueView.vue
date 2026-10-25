<template>
  <!-- 全屏沉浸式复习：一次一条，完成即推进一轮（无自判，产品文档 §6） -->
  <div class="immersive">
    <button class="exit" title="退出复习" @click="$router.push('/')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
    </button>

    <div v-if="loading" class="state dim">加载中…</div>

    <template v-else-if="queue.length">
      <div class="progress-label dim num">{{ index + 1 }} / {{ queue.length }}</div>

      <article class="review-card glass-strong">
        <header class="card-head">
          <span class="badge" :class="cur.type === 1 ? 'badge-note' : 'badge-wrong'">
            {{ cur.type === 1 ? 'note' : '错题' }}
          </span>
          <span class="subject">{{ cur.subjectName }}</span>
          <span class="round dim">第 {{ cur.round + 1 }} 轮复习</span>
        </header>

        <p class="content">{{ cur.content }}</p>

        <div v-if="cur.images && cur.images.length" class="imgs">
          <img v-for="im in cur.images" :key="im.objectKey" :src="im.previewUrl" alt="" @click="openImg(im.previewUrl)" />
        </div>

        <div v-if="cur.tags && cur.tags.length" class="tags">
          <span v-for="t in cur.tags" :key="t" class="mini-tag">#{{ t }}</span>
        </div>

        <footer class="card-foot">
          <button class="btn-amber complete-btn" :disabled="busy" @click="onComplete">
            {{ busy ? '提交中…' : '完成复习' }}
          </button>
        </footer>
      </article>
    </template>

    <!-- 全部复习完 -->
    <div v-else class="state done-state">
      <p class="big">本轮复习全部完成</p>
      <p class="dim">坚持本身就是最强的记忆方法</p>
      <button class="btn-amber" @click="$router.push('/')">回到主界面</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDueReviews, completeReview } from '@/api'
import { toast } from '@/utils/toast'

const queue = ref([])
const index = ref(0)
const loading = ref(true)
const busy = ref(false)

const cur = computed(() => queue.value[index.value])

function openImg(src) {
  // 轻量放大：直接新开（v1 不做灯箱）
  if (src && !src.startsWith('blob:')) window.open(src, '_blank')
}

async function load() {
  loading.value = true
  try {
    queue.value = await getDueReviews()
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

async function onComplete() {
  if (busy.value) return
  busy.value = true
  try {
    // 契约 §6.2：无自判，完成即 round+1 顺延
    await completeReview(cur.value.id)
    if (index.value < queue.value.length - 1) {
      index.value += 1
    } else {
      queue.value = [] // 全部完成 → 完成态
      toast('今日到期已全部复习完')
    }
  } catch (e) {
    toast(e.message)
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.immersive {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
}

.exit {
  position: fixed;
  top: 22px;
  right: 22px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: center;
}
.exit svg { width: 17px; height: 17px; }

.progress-label {
  font-size: 14px;
  letter-spacing: 2px;
  margin-bottom: 16px;
}

.review-card {
  width: min(820px, 94vw);
  padding: 34px 40px 30px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 13px;
}
.subject { letter-spacing: 1px; }
.round { margin-left: auto; }

.content {
  margin: 22px 0;
  font-size: 18px;
  line-height: 1.95;
  white-space: pre-wrap;
}

.imgs { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.imgs img {
  width: 108px;
  height: 108px;
  object-fit: cover;
  border-radius: 10px;
  cursor: zoom-in;
}

.tags { display: flex; flex-wrap: wrap; gap: 7px; }
.mini-tag {
  font-size: 12px;
  color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.12);
  padding: 3px 11px;
  border-radius: 999px;
}

.card-foot { margin-top: 28px; text-align: center; }
.complete-btn { padding: 13px 42px; font-size: 16px; }

.state { text-align: center; font-size: 15px; }
.done-state .big { font-size: 22px; letter-spacing: 2px; margin-bottom: 10px; }
.done-state .btn-amber { margin-top: 26px; }
</style>
