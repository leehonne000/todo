<template>
  <div class="page page-narrow">
    <TopBar title="文章" back-to="/info" />

    <article v-if="article" class="article glass">
      <h1 class="title">{{ article.title }}</h1>
      <p class="date dim num">{{ (article.publishedAt || '').replace('T', ' ').slice(0, 10) }}<span v-if="article.authorName" class="author"> · {{ article.authorName }}</span></p>
      <img v-if="article.coverUrl" class="cover" :src="article.coverUrl" alt="" />
      <div class="content">{{ article.content }}</div>

      <!-- 留言按钮（文章右下角）：自己文章不显示（不能给自己的文章留言） -->
      <div v-if="!isOwnArticle" class="msg-foot">
        <button class="msg-btn" type="button" @click="toggleMsg">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.8-.9L3 20l1.1-4.9A8.4 8.4 0 0 1 3.1 11 8.5 8.5 0 0 1 12 2.5a8.4 8.4 0 0 1 9 9z" />
          </svg>
          {{ msgOpen ? '收起留言' : '留言' }}
        </button>
      </div>
    </article>
    <p v-else class="empty dim">加载中…</p>

    <!-- 留言面板：与作者管理员的私密对话串（仅你和对方可见） -->
    <section v-if="article && msgOpen" class="msg-panel glass">
      <p class="msg-hint dim">与 {{ article.authorName }} 的私密留言 · 仅你和对方可见</p>

      <div class="msg-list">
        <div v-for="m in thread" :key="m.id" class="msg-row" :class="m.senderRole === 0 ? 'mine' : 'theirs'">
          <img class="msg-avatar" :src="m.senderAvatar || '/avatar-default.png'" alt="" @error="onAvatarErr" />
          <div class="bubble-wrap">
            <p class="bubble-name dim">{{ m.senderName }}</p>
            <div class="bubble">
              <p class="bubble-text">{{ m.content }}</p>
              <p class="bubble-time dim num">{{ fmtTime(m.createdAt) }}</p>
            </div>
          </div>
        </div>
        <p v-if="!thread.length" class="empty dim">还没有留言，说点什么吧</p>
      </div>

      <div class="msg-input-row">
        <textarea
          v-model="draft" class="msg-input" rows="2" maxlength="300"
          placeholder="给作者留言（1~300 字）"
        ></textarea>
        <button class="send-btn" type="button" :disabled="sending || !draft.trim()" @click="send">发送</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { getArticle, sendMessage, getMessageThread } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const route = useRoute()
const auth = useAuthStore()
const article = ref(null)

// ---------- 留言（读者来信，2026-10-28） ----------
const msgOpen = ref(false)
const thread = ref([])
const draft = ref('')
const sending = ref(false)

// 自己（作者管理员）看自己的文章时不显示留言入口
const isOwnArticle = computed(() =>
  article.value?.authorId != null && auth.userId != null && article.value.authorId === auth.userId
)

function fmtTime(t) {
  return (t || '').replace('T', ' ').slice(0, 16)
}

// 头像加载失败兜底（裂图 → 默认头像，只兜一次防死循环）
function onAvatarErr(e) {
  const img = e.target
  if (img.dataset.fallback) return
  img.dataset.fallback = '1'
  img.src = '/avatar-default.png'
}

async function toggleMsg() {
  msgOpen.value = !msgOpen.value
  if (msgOpen.value) await loadThread()
}

async function loadThread() {
  try {
    const data = await getMessageThread(route.params.id)
    thread.value = data.messages || []
  } catch (e) {
    toast(e.message)
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  if (content.length > 300) return toast('留言最多 300 字')
  sending.value = true
  try {
    const m = await sendMessage(route.params.id, content)
    thread.value.push(m)
    draft.value = ''
    toast('留言已发送，仅你和作者可见')
  } catch (e) {
    toast(e.message)
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  try {
    article.value = await getArticle(route.params.id)
  } catch (e) {
    toast(e.message)
  }
})
</script>

<style scoped>
.article { width: 100%; padding: 28px 30px; }
.title { font-size: 22px; font-weight: 500; line-height: 1.5; letter-spacing: 1px; }
.date { margin: 10px 0 18px; font-size: 12.5px; }
.author { color: var(--theme-text); }
.cover {
  width: 100%;
  max-height: 280px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 20px;
}
.content {
  font-size: 15.5px;
  line-height: 2;
  white-space: pre-wrap;
  color: rgba(255, 255, 255, 0.92);
}
.empty { text-align: center; padding: 40px 0; }

/* ---------- 留言（读者来信） ---------- */
.msg-foot { display: flex; justify-content: flex-end; margin-top: 22px; }
.msg-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 20px;
  font-size: 13.5px;
  letter-spacing: 1px;
  border-radius: 999px;
  border: 1px solid var(--theme);
  background: transparent;
  color: var(--theme-text);
  cursor: pointer;
  transition: background 0.15s;
}
.msg-btn:hover { background: var(--glass-bg-strong); }
.msg-btn svg { width: 16px; height: 16px; }

.msg-panel { width: 100%; margin-top: 14px; padding: 20px 22px; }
.msg-hint { font-size: 12px; letter-spacing: 1px; margin-bottom: 14px; }

.msg-list { display: flex; flex-direction: column; gap: 14px; max-height: 380px; overflow-y: auto; padding-right: 4px; }
.msg-row { display: flex; gap: 8px; align-items: flex-start; }
.msg-row.mine { flex-direction: row-reverse; }
.msg-avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 50%;
  object-fit: cover;
}
.bubble-wrap { max-width: 78%; display: flex; flex-direction: column; }
.msg-row.mine .bubble-wrap { align-items: flex-end; }
.bubble-name { font-size: 11px; margin: 0 4px 3px; opacity: 0.75; }
.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}
.msg-row.mine .bubble {
  background: var(--glass-bg-strong);
  color: var(--text);
  border-bottom-right-radius: 4px;
}
.msg-row.theirs .bubble {
  background: var(--glass-bg-strong);
  color: var(--text);
  border-bottom-left-radius: 4px;
}
.bubble-text { white-space: pre-wrap; word-break: break-word; }
.bubble-time { font-size: 10.5px; margin-top: 4px; opacity: 0.7; text-align: right; }

.msg-input-row { display: flex; gap: 10px; margin-top: 16px; align-items: flex-end; }
.msg-input {
  flex: 1;
  resize: none;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: var(--glass-bg);
  color: var(--text);
}
.msg-input:focus { outline: none; border-color: var(--theme); }
.send-btn {
  padding: 11px 22px;
  font-size: 13.5px;
  letter-spacing: 2px;
  border-radius: 10px;
  border: none;
  background: var(--theme);
  color: var(--theme-on);
  cursor: pointer;
}
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
