<template>
  <!-- 沉浸刷题：把自由库当前筛选的全部内容逐条播放（版式同 Review；纯浏览，不推进艾宾浩斯） -->
  <div class="immersive">
    <button class="exit" title="退出刷题" @click="$router.push('/library')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
    </button>

    <div v-if="loading" class="state dim">加载中…</div>

    <template v-else-if="ids.length && !done">
      <div class="progress-label dim num">{{ index + 1 }} / {{ ids.length }}</div>

      <article v-if="cur" class="review-card glass-strong">
        <header class="card-head">
          <span class="badge" :class="cur.type === 1 ? 'badge-note' : 'badge-wrong'">
            {{ cur.type === 1 ? 'note' : '错题' }}
          </span>
          <span class="subject">{{ cur.subjectName }}</span>
          <span v-if="cur.round > 0" class="round dim">已复习 {{ cur.round }} 轮</span>
        </header>

        <p class="content">{{ cur.content || '（纯图片记录）' }}</p>

        <div v-if="cur.images && cur.images.length" class="imgs">
          <img v-for="im in cur.images" :key="im.objectKey" :src="im.previewUrl" alt="" @click="openImg(im.previewUrl)" />
        </div>

        <div v-if="cur.tags && cur.tags.length" class="tags">
          <span v-for="t in cur.tags" :key="t" class="mini-tag">#{{ t }}</span>
        </div>

        <footer class="card-foot">
          <button class="btn-amber next-btn" @click="onNext">
            {{ index < ids.length - 1 ? '下一条' : '完成' }}
          </button>
        </footer>
      </article>
      <div v-else class="state dim">加载中…</div>
    </template>

    <!-- 空筛选 -->
    <div v-else-if="!ids.length" class="state done-state">
      <p class="big">当前筛选没有内容</p>
      <button class="btn-amber" @click="$router.push('/library')">返回自由库</button>
    </div>

    <!-- 全部播完 -->
    <div v-else class="state done-state">
      <p class="big">沉浸刷题完成</p>
      <p class="dim">这组内容全部过了一遍，明天再来一遍会更熟</p>
      <button class="btn-amber" @click="$router.push('/library')">返回自由库</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEntries, getEntry } from '@/api'
import { toast } from '@/utils/toast'

const route = useRoute()

const ids = ref([])
const index = ref(0)
const cur = ref(null)
const loading = ref(true)
const done = ref(false)

const hasNext = computed(() => index.value < ids.value.length - 1)

function openImg(src) {
  if (src && !src.startsWith('blob:')) window.open(src, '_blank')
}

/** 取当前筛选的全部记录 id（跨页收齐，上限 1000 条） */
async function loadAll() {
  const base = {
    subjectId: route.query.subjectId || null,
    type: route.query.type || null,
    size: 50
  }
  let all = []
  let page = 1
  let total = 0
  try {
    do {
      const data = await getEntries({ ...base, page })
      all = all.concat(data.items)
      total = data.total
      page += 1
    } while (all.length < total && page <= 20)
    ids.value = all.map(it => it.id)
    if (ids.value.length) await show(0)
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

/** 逐条取详情（图片走预签名 URL） */
async function show(i) {
  index.value = i
  cur.value = null
  try {
    cur.value = await getEntry(ids.value[i])
  } catch (e) {
    toast(e.message)
  }
}

function onNext() {
  if (hasNext.value) {
    show(index.value + 1)
  } else {
    done.value = true
  }
}

onMounted(loadAll)
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
.next-btn { padding: 13px 42px; font-size: 16px; }

.state { text-align: center; font-size: 15px; }
.done-state .big { font-size: 22px; letter-spacing: 2px; margin-bottom: 10px; }
.done-state .btn-amber { margin-top: 26px; }
</style>
