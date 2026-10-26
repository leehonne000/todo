<template>
  <div class="page page-narrow">
    <TopBar :title="type === 1 ? '编辑 note' : '编辑错题'" />

    <!-- 文字内容：可空（纯图片记录） -->
    <textarea
      v-model="content"
      class="content-area glass"
      rows="7"
      placeholder="写点什么…（可留空，纯图片记录也行）"
    ></textarea>

    <!-- 图片：＋虚线方块 + 72px 缩略图，上限 10 张 -->
    <div class="section-label dim">图片（{{ images.length }}/10）</div>
    <div class="images">
      <div v-for="(im, i) in images" :key="im.objectKey" class="thumb">
        <img :src="im.previewUrl" alt="" @click="openEditor(i)" />
        <span class="edit-badge" @click="openEditor(i)">✎</span>
        <button class="del" @click="removeImage(i)">×</button>
      </div>
      <button v-if="images.length < 10" class="add-img" @click="fileInput?.click()">
        <span>＋</span>
      </button>
      <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
    </div>

    <ImageEditor v-model:show="editorShow" :src="editorSrc" @apply="onEditorApply" />

    <!-- 自定义标签 -->
    <div class="section-label dim">标签</div>
    <div class="tags">
      <span v-for="(t, i) in tagNames" :key="t" class="chip">
        {{ t }}
        <button @click="tagNames.splice(i, 1)">×</button>
      </span>
      <button v-if="!tagInputOpen" class="add-tag" @click="openTagInput">＋标签</button>
    </div>
    <input
      v-if="tagInputOpen"
      ref="tagInput"
      v-model.trim="tagDraft"
      class="field tag-field"
      placeholder="输入标签名，回车确认"
      @keyup.enter="addTag"
      @blur="addTag"
    />

    <button class="btn-amber save-btn" :disabled="saving" @click="onSave">
      {{ saving ? '上传中…' : '完成' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import ImageEditor from '@/components/ImageEditor.vue'
import { useLearnStore } from '@/stores/learn'
import { uploadImage } from '@/api'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const learn = useLearnStore()

const type = ref(Number(route.query.type) || 1)
const editIndex = ref(Number(route.query.index))
if (Number.isNaN(editIndex.value)) editIndex.value = -1

const content = ref('')
const images = ref([]) // { objectKey, previewUrl, localFile?, localUrl? }
const tagNames = ref([])
const tagInputOpen = ref(false)
const tagDraft = ref('')
const saving = ref(false)
const fileInput = ref(null)
const tagInput = ref(null)

// 图片编辑器
const editorShow = ref(false)
const editorSrc = ref('')
let editingIndex = -1

// 进入时回填（编辑已有草稿）
onMounted(() => {
  const idx = editIndex.value
  if (idx >= 0 && idx < learn.drafts.length) {
    const d = learn.drafts[idx]
    content.value = d.content
    tagNames.value = [...d.tagNames]
    images.value = d.imageObjectKeys.map((k, i) => ({
      objectKey: k,
      previewUrl: (d.imagePreviews && d.imagePreviews[i]) || k
    }))
    type.value = d.type
  }
})

function openTagInput() {
  tagInputOpen.value = true
  nextTick(() => tagInput.value?.focus())
}

function addTag() {
  const t = tagDraft.value.trim()
  if (t && !tagNames.value.includes(t)) {
    if (tagNames.value.length >= 10) return toast('标签别太多，10 个够用了')
    tagNames.value.push(t)
  }
  tagDraft.value = ''
  tagInputOpen.value = false
}

async function onFileChange(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (images.value.length >= 10) return toast('最多 10 张')
  saving.value = true
  try {
    // 契约 §5.5：先传图拿 objectKey，Done 时随条目归档
    const data = await uploadImage(file)
    images.value.push({
      objectKey: data.objectKey,
      previewUrl: data.previewUrl,
      localFile: file,
      localUrl: URL.createObjectURL(file)
    })
  } catch (err) {
    toast(err.message)
  } finally {
    saving.value = false
  }
}

/** 打开图片编辑器：优先编辑本地原图（避开跨域画布限制） */
function openEditor(i) {
  const im = images.value[i]
  if (!im) return
  editingIndex = i
  editorSrc.value = im.localUrl || im.previewUrl
  editorShow.value = true
}

/** 编辑应用：重新上传成品并替换该项 */
async function onEditorApply(blob) {
  const i = editingIndex
  if (i < 0 || i >= images.value.length) return
  saving.value = true
  try {
    const file = new File([blob], 'edited.jpg', { type: 'image/jpeg' })
    const data = await uploadImage(file)
    images.value[i] = {
      objectKey: data.objectKey,
      previewUrl: data.previewUrl,
      localFile: file,
      localUrl: URL.createObjectURL(blob)
    }
    toast('编辑已应用')
  } catch (err) {
    toast(err.message)
  } finally {
    saving.value = false
  }
}

function removeImage(i) {
  images.value.splice(i, 1)
}

function onSave() {
  const text = content.value.trim()
  if (!text && images.value.length === 0) return toast('文字和图片至少留一样')
  const draft = {
    type: type.value,
    content: text,
    tagNames: [...tagNames.value],
    imageObjectKeys: images.value.map(im => im.objectKey),
    imagePreviews: images.value.map(im => im.previewUrl)
  }
  if (editIndex.value >= 0) learn.updateDraft(editIndex.value, draft)
  else learn.addDraft(draft)
  router.back()
}
</script>

<style scoped>
.content-area {
  width: 100%;
  padding: 16px;
  font-size: 15px;
  line-height: 1.8;
  resize: vertical;
  min-height: 150px;
}
.content-area::placeholder { color: var(--text-faint); }

.section-label { margin: 20px 0 10px; font-size: 13px; }

.images { display: flex; flex-wrap: wrap; gap: 10px; }
.thumb {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; cursor: pointer; }
.thumb .edit-badge {
  position: absolute;
  left: 2px;
  bottom: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.thumb .del {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  border: 1.5px dashed rgba(255, 255, 255, 0.35);
  color: var(--text-dim);
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-img:hover { border-color: var(--theme-text); color: var(--theme-text); }

.tags { display: flex; flex-wrap: wrap; gap: 9px; align-items: center; }
.add-tag {
  padding: 7px 15px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-dim);
  border: 1.5px dashed rgba(255, 255, 255, 0.3);
}
.add-tag:hover { color: var(--theme-text); border-color: var(--theme-text); }
.tag-field { margin-top: 10px; }

.save-btn {
  margin-top: 30px;
  width: 100%;
  padding: 15px;
  font-size: 16px;
}
</style>
