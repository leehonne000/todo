<template>
  <AdminShell>
    <div class="page-head">
      <div>
        <h1>留言管理</h1>
        <div class="sub">文章下的读者来信——用户给你的私密留言；回复仅该用户可见。{{ auth.isSuperAdmin ? '超管可见全部管理员的留言。' : '仅显示你自己文章的留言（管理员之间互不可见）。' }}</div>
      </div>
    </div>

    <!-- 超管筛选：按管理员（2026-10-28 业务定稿：默认全部、可按管理员筛） -->
    <div v-if="auth.isSuperAdmin" class="filter-row">
      <TabRow v-model="authorFilter" :items="authorTabs" />
    </div>

    <div class="msg-layout">
      <!-- 左：会话列表 -->
      <div class="thread-list glass">
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="!threads.length" class="empty">还没有留言</div>
        <button
          v-for="t in threads" :key="`${t.articleId}:${t.userId}`"
          class="thread-item" :class="{ active: isActive(t) }"
          type="button" @click="openThread(t)"
        >
          <img class="thread-avatar" :src="t.userAvatar || '/avatar-default.png'" alt="" @error="onAvatarErr" />
          <div class="thread-body">
            <div class="thread-top">
              <span class="thread-user">{{ t.userName }}</span>
              <span class="thread-time dim num">{{ fmtTime(t.lastAt) }}</span>
            </div>
            <div class="thread-title dim">{{ t.articleTitle }}</div>
            <div class="thread-last">
              <span v-if="t.lastFrom === 1" class="from-tag">{{ t.authorId === auth.userId ? '我' : t.authorName }}</span>
              <span class="last-text">{{ t.lastContent }}</span>
              <span v-if="t.unreadCount > 0" class="unread-chip num">{{ t.unreadCount }}</span>
            </div>
            <div v-if="auth.isSuperAdmin" class="thread-owner dim">作者：{{ t.authorName }}</div>
          </div>
        </button>
      </div>

      <!-- 右：对话区 -->
      <div class="thread-pane glass">
        <template v-if="current">
          <div class="pane-head">
            <img class="pane-avatar" :src="current.userAvatar || '/avatar-default.png'" alt="" @error="onAvatarErr" />
            <div>
              <div class="pane-user">{{ current.userName }} <span v-if="current.userEmail" class="dim">{{ current.userEmail }}</span></div>
              <div class="pane-article dim">《{{ current.articleTitle }}》</div>
            </div>
          </div>

          <div ref="listEl" class="pane-list">
            <div v-for="m in current.messages" :key="m.id" class="row" :class="m.senderRole === 0 ? 'from-user' : 'from-admin'">
              <img class="row-avatar" :src="m.senderAvatar || '/avatar-default.png'" alt="" @error="onAvatarErr" />
              <div class="row-body">
                <p class="b-name dim">{{ m.senderName }}</p>
                <div class="bubble">
                  <p class="b-text">{{ m.content }}</p>
                  <div class="b-foot">
                    <span class="b-time dim num">{{ fmtTime(m.createdAt) }}</span>
                    <button class="b-del" type="button" title="删除这条留言" @click="askDelete(m)">×</button>
                  </div>
                </div>
              </div>
            </div>
            <p v-if="!current.messages.length" class="empty dim">对话为空</p>
          </div>

          <!-- 回复权=仅文章作者本人（超管可看全部但不可代回复，2026-10-28 权限定稿） -->
          <div v-if="canReply" class="pane-input">
            <textarea v-model="draft" class="reply-input" rows="2" maxlength="300"
                      placeholder="回复用户（仅该用户可见，1~300 字）"></textarea>
            <button class="btn-amber send" type="button" :disabled="busy || !draft.trim()" @click="onReply">发送</button>
          </div>
          <p v-else class="no-reply dim">这条会话发给了 {{ current.authorName }}，只有 TA 能回复（你可以查看和删除留言）</p>
        </template>
        <div v-else class="empty dim">从左侧选择一条留言开始处理</div>
      </div>
    </div>

    <Modal v-model="showDelete" title="删除留言"
           confirm-text="删除" @confirm="onDelete">
      <p style="line-height:1.8;font-size:14px;">
        确定删除这条留言吗？<br />
        <span class="dim">删除后不可恢复（用户侧对话同步消失）。</span>
      </p>
    </Modal>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import AdminShell from '@/components/AdminShell.vue'
import TabRow from '@/components/TabRow.vue'
import Modal from '@/components/Modal.vue'
import { adminListMessages, adminGetMessageThread, adminReplyMessage, adminDeleteMessage } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const auth = useAuthStore()

const threads = ref([])
const loading = ref(false)
const current = ref(null) // {articleId,userId,articleTitle,userName,userEmail,messages[]}
const draft = ref('')
const busy = ref(false)
const listEl = ref(null)

const showDelete = ref(false)
const pendingDelete = ref(null)

// 超管筛选（业务定稿：默认全部、可按管理员筛）；普通管理员只有一个固定范围
// 顺序定稿（2026-10-28）：全部 → 我自己 → 其他管理员
const authorFilter = ref('all')
const authorTabs = computed(() => {
  const seen = new Map()
  for (const t of threads.value) {
    if (t.authorId != null && !seen.has(t.authorId)) seen.set(t.authorId, t.authorName)
  }
  const rest = [...seen.entries()].sort(([a], [b]) => {
    if (a === auth.userId) return -1
    if (b === auth.userId) return 1
    return 0
  })
  return [
    { value: 'all', label: '全部' },
    ...rest.map(([id, name]) => ({ value: id, label: name }))
  ]
})

function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : '—'
}
// 头像加载失败兜底（裂图 → 默认头像，只兜一次防死循环）
function onAvatarErr(e) {
  const img = e.target
  if (img.dataset.fallback) return
  img.dataset.fallback = '1'
  img.src = '/avatar-default.png'
}
function isActive(t) {
  return current.value && current.value.articleId === t.articleId && current.value.userId === t.userId
}
// 回复权：仅会话归属文章的作者本人（后端同口径，超管不可代回复）
const canReply = computed(() => !!current.value && current.value.authorId === auth.userId)

async function load() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (auth.isSuperAdmin && authorFilter.value !== 'all') params.authorId = authorFilter.value
    const data = await adminListMessages(params)
    threads.value = data.items || []
  } catch (e) {
    toast(e.message)
  } finally {
    loading.value = false
  }
}

async function openThread(t) {
  try {
    const data = await adminGetMessageThread(t.articleId, t.userId)
    current.value = data
    draft.value = ''
    // 打开即已读：清本会话红点 + 刷新列表/侧栏红点
    t.unreadCount = 0
    window.dispatchEvent(new CustomEvent('messages-unread-refresh'))
    await nextTick()
    if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
  } catch (e) {
    toast(e.message)
  }
}

async function onReply() {
  const content = draft.value.trim()
  if (!content || !current.value) return
  if (content.length > 300) return toast('留言最多 300 字')
  busy.value = true
  try {
    const m = await adminReplyMessage(current.value.articleId, current.value.userId, content)
    current.value.messages.push(m)
    draft.value = ''
    toast('已回复，仅该用户可见')
    await nextTick()
    if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
  } catch (e) {
    toast(e.message)
  } finally {
    busy.value = false
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
    await adminDeleteMessage(m.id)
    if (current.value) {
      current.value.messages = current.value.messages.filter(x => x.id !== m.id)
    }
    toast('已删除')
    load() // 刷新左侧摘要
    window.dispatchEvent(new CustomEvent('messages-unread-refresh'))
  } catch (e) {
    toast(e.message)
  } finally {
    showDelete.value = false
    pendingDelete.value = null
  }
}

watch(authorFilter, () => {
  current.value = null
  load()
})

onMounted(load)
</script>

<style scoped>
/* ---------- 左右分栏 ---------- */
.msg-layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
  min-height: 480px;
}
.thread-list {
  width: 320px;
  flex: none;
  padding: 10px;
  overflow-y: auto;
  max-height: 620px;
}
.thread-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
  max-height: 620px;
}

/* ---------- 会话列表 ---------- */
.thread-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  width: 100%;
  text-align: left;
  padding: 12px 13px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  transition: background 0.15s;
}
.thread-avatar {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 50%;
  object-fit: cover;
}
.thread-body { flex: 1; min-width: 0; }
.thread-item:hover { background: rgba(255, 255, 255, 0.08); }
.thread-item.active { background: rgba(255, 255, 255, 0.13); }
.thread-top { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; }
.thread-user { font-size: 14px; font-weight: 600; }
.thread-time { font-size: 11px; flex: none; }
.thread-title {
  margin-top: 3px;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.thread-last {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
}
.from-tag {
  flex: none;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.12);
  color: var(--theme-text);
}
.last-text {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.85;
}
.unread-chip {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #e5484d;
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.thread-owner { margin-top: 5px; font-size: 11px; }

/* ---------- 对话区 ---------- */
.pane-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.pane-avatar {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 50%;
  object-fit: cover;
}
.pane-user { font-size: 15px; font-weight: 600; }
.pane-user .dim { font-size: 12px; font-weight: 400; margin-left: 6px; }
.pane-article { margin-top: 4px; font-size: 12.5px; }

.pane-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 2px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.row { display: flex; gap: 8px; align-items: flex-start; }
.row.from-admin { flex-direction: row-reverse; }
.row-avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 50%;
  object-fit: cover;
}
.row-body { max-width: 72%; display: flex; flex-direction: column; }
.row.from-admin .row-body { align-items: flex-end; }
.b-name { font-size: 11px; margin: 0 4px 3px; opacity: 0.75; }
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}
.row.from-user .bubble {
  background: rgba(255, 255, 255, 0.12);
  border-bottom-left-radius: 4px;
}
.row.from-admin .bubble {
  background: rgba(255, 255, 255, 0.12);
  border-bottom-right-radius: 4px;
}
.b-text { white-space: pre-wrap; word-break: break-word; }
.b-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}
.b-time { font-size: 10.5px; opacity: 0.75; }
.b-del {
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
  transition: opacity 0.15s;
}
.bubble:hover .b-del { opacity: 0.7; }
.b-del:hover { opacity: 1 !important; }

.no-reply {
  padding: 14px 0 2px;
  font-size: 12.5px;
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.pane-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.reply-input {
  flex: 1;
  resize: none;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.06);
  color: var(--text);
}
.reply-input:focus { outline: none; border-color: var(--theme); }
.send { flex: none; padding: 11px 26px; font-size: 13.5px; letter-spacing: 2px; }

.empty { text-align: center; padding: 40px 0; font-size: 13.5px; }
.filter-row { margin-bottom: 14px; }
</style>
