<template>
  <div class="page page-narrow">
    <TopBar :title="editing ? '编辑记录' : '记录详情'">
      <div class="actions">
        <button v-if="!editing" class="mini-btn" @click="startEdit">编辑</button>
        <button v-if="!editing" class="mini-btn danger" @click="onDelete">删除</button>
        <button v-if="editing" class="mini-btn" @click="editing = false">取消</button>
        <button v-if="editing" class="mini-btn amber" @click="onSave">保存</button>
      </div>
    </TopBar>

    <div v-if="entry" class="detail glass">
      <header class="head">
        <span class="badge" :class="entry.type === 1 ? 'badge-note' : 'badge-wrong'">
          {{ entry.type === 1 ? 'note' : '错题' }}
        </span>
        <span class="subject dim">{{ entry.subjectName }}</span>
        <span class="date dim num">{{ dayLabel }}</span>
      </header>

      <!-- 查看态 -->
      <template v-if="!editing">
        <p class="content">{{ entry.content || '（纯图片记录）' }}</p>
        <div v-if="entry.images.length" class="imgs">
          <img v-for="im in entry.images" :key="im.objectKey" :src="im.previewUrl" alt="" />
        </div>
        <div v-if="entry.tags.length" class="tags">
          <span v-for="t in entry.tags" :key="t" class="mini-tag">#{{ t }}</span>
        </div>
        <div class="meta dim">
          复习进度 <b class="amber num">{{ entry.round }}</b> / 6 轮
          <span v-if="entry.reviewStatus === 1" class="out">已出队</span>
          <span v-else>· 编辑不改变复习节奏</span>
        </div>
      </template>

      <!-- 编辑态（整体替换式，契约 §7.3） -->
      <template v-else>
        <textarea v-model="editContent" class="content-area" rows="7" placeholder="文字内容…"></textarea>

        <div class="section-label dim">图片（{{ editImages.length }}/10）</div>
        <div class="imgs">
          <div v-for="(im, i) in editImages" :key="im.objectKey" class="thumb">
            <img :src="im.previewUrl" alt="" />
            <button class="del" @click="editImages.splice(i, 1)">×</button>
          </div>
          <button v-if="editImages.length < 10" class="add-img" @click="fileInput?.click()">＋</button>
          <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
        </div>

        <div class="section-label dim">标签</div>
        <div class="tags">
          <span v-for="(t, i) in editTags" :key="t" class="chip">{{ t }} <button @click="editTags.splice(i, 1)">×</button></span>
          <button class="add-tag" @click="openTagInput">＋标签</button>
        </div>
        <input
          v-if="tagInputOpen" ref="tagInput" v-model.trim="tagDraft" class="field tag-field"
          placeholder="回车确认" @keyup.enter="addTag" @blur="addTag"
        />
      </template>
    </div>

    <p v-else class="empty dim">加载中…</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { getEntry, updateEntry, deleteEntry, uploadImage } from '@/api'
import { dayOf } from '@/utils/format'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const entry = ref(null)
// 归属学习日 + 时刻（凌晨 4 点前算前一天）
const dayLabel = computed(() => {
  if (!entry.value) return ''
  return `${dayOf(entry.value.createdAt)} ${String(entry.value.createdAt).slice(11, 16)}`
})
const editing = ref(false)
const editContent = ref('')
const editImages = ref([])
const editTags = ref([])
const tagInputOpen = ref(false)
const tagDraft = ref('')
const fileInput = ref(null)
const tagInput = ref(null)

async function load() {
  try {
    entry.value = await getEntry(route.params.id)
  } catch (e) {
    toast(e.message)
  }
}

function startEdit() {
  editContent.value = entry.value.content
  editImages.value = entry.value.images.map(im => ({ ...im }))
  editTags.value = [...entry.value.tags]
  editing.value = true
}

function openTagInput() {
  tagInputOpen.value = true
  nextTick(() => tagInput.value?.focus())
}
function addTag() {
  const t = tagDraft.value.trim()
  if (t && !editTags.value.includes(t)) editTags.value.push(t)
  tagDraft.value = ''
  tagInputOpen.value = false
}

async function onFileChange(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  try {
    const data = await uploadImage(file)
    editImages.value.push({ objectKey: data.objectKey, previewUrl: data.previewUrl })
  } catch (err) {
    toast(err.message)
  }
}

async function onSave() {
  if (!editContent.value.trim() && editImages.value.length === 0) return toast('文字和图片至少留一样')
  try {
    await updateEntry(entry.value.id, {
      content: editContent.value.trim(),
      tagNames: editTags.value,
      imageObjectKeys: editImages.value.map(im => im.objectKey)
    })
    toast('已保存')
    editing.value = false
    await load()
  } catch (e) {
    toast(e.message)
  }
}

async function onDelete() {
  if (!confirm('确定删除这条记录？删除后不可恢复。')) return
  try {
    await deleteEntry(entry.value.id)
    toast('已删除')
    router.back()
  } catch (e) {
    toast(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.actions { margin-left: auto; display: flex; gap: 8px; }
.mini-btn {
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 13px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
}
.mini-btn.danger { color: #f09595; border-color: rgba(240, 149, 149, 0.4); }
.mini-btn.amber { color: var(--theme-text); border-color: rgb(var(--theme-rgb) / 0.5); }

.detail { width: 100%; padding: 22px 24px; }
.head { display: flex; align-items: center; gap: 11px; font-size: 12.5px; flex-wrap: wrap; }
.date { margin-left: auto; }

.content {
  margin: 18px 0;
  font-size: 16px;
  line-height: 1.95;
  white-space: pre-wrap;
}
.content-area {
  width: 100%;
  margin: 14px 0;
  padding: 14px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
  resize: vertical;
}

.imgs { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.imgs img { width: 108px; height: 108px; object-fit: cover; border-radius: 10px; display: block; }
.thumb { position: relative; width: 72px; height: 72px; border-radius: 10px; overflow: hidden; }
.thumb img { width: 72px; height: 72px; }
.thumb .del {
  position: absolute; top: 2px; right: 2px;
  width: 20px; height: 20px; border-radius: 50%;
  background: rgba(0, 0, 0, 0.55); color: #fff; font-size: 14px;
}
.add-img {
  width: 72px; height: 72px; border-radius: 10px;
  border: 1.5px dashed rgba(255, 255, 255, 0.35);
  color: var(--text-dim); font-size: 22px;
}

.tags { display: flex; flex-wrap: wrap; gap: 7px; align-items: center; }
.mini-tag {
  font-size: 12px; color: var(--theme-text);
  background: rgb(var(--theme-rgb) / 0.12);
  padding: 3px 11px; border-radius: 999px;
}
.add-tag {
  padding: 6px 14px; border-radius: 999px; font-size: 12.5px;
  color: var(--text-dim); border: 1.5px dashed rgba(255, 255, 255, 0.3);
}
.tag-field { margin-top: 10px; }
.section-label { margin: 16px 0 8px; font-size: 13px; }

.meta { margin-top: 18px; font-size: 12.5px; }
.meta b { font-weight: 500; }
.out { color: var(--text-faint); }
.empty { text-align: center; padding: 40px 0; }
</style>
