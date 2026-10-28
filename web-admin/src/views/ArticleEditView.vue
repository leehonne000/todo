<template>
  <AdminShell>
    <div class="page-head">
      <div>
        <h1>{{ isNew ? '写新文章' : '编辑文章' }}</h1>
        <div class="sub">
          {{ isNew ? '保存后先存为草稿，确认无误再发布' : `文章 #${id}` }}
          <span v-if="!isNew" class="status" :class="status === 1 ? 'status-pub' : 'status-draft'" style="margin-left:10px;">
            {{ status === 1 ? '已发布' : '草稿' }}
          </span>
        </div>
      </div>
      <div class="actions">
        <button class="btn-glass" type="button" @click="$router.push('/articles')">返回列表</button>
        <button class="btn-amber" type="button" :disabled="saving" @click="onSave">
          {{ saving ? '保存中…' : (status === 1 ? '保存修改' : '存草稿') }}
        </button>
        <button v-if="status !== 1" class="btn-glass" type="button" :disabled="saving" @click="onSavePublish">保存并发布</button>
        <button v-else class="btn-glass" type="button" :disabled="saving" @click="onUnpublish">下线</button>
      </div>
    </div>

    <div class="card glass">
      <div class="form-row" style="margin-bottom:18px;">
        <label>标题（≤120 字）</label>
        <input v-model="title" class="field" type="text" maxlength="120" placeholder="给文章起个标题" />
      </div>

      <div class="form-row" style="margin-bottom:18px;">
        <label>正文（v1 纯文本 / Markdown 原文）</label>
        <textarea v-model="content" class="field" placeholder="写点对考研人有用的东西…"></textarea>
      </div>

      <div class="form-row">
        <label>封面（可选）</label>
        <div class="cover-box">
          <img v-if="cover" class="cover-thumb" :src="cover.url" alt="封面预览" />
          <div v-else class="cover-empty">无封面</div>
          <div class="cover-ops">
            <button class="op-btn primary" type="button" :disabled="uploading" @click="pickFile">
              {{ uploading ? '上传中…' : (cover ? '换一张' : '上传封面') }}
            </button>
            <button v-if="cover" class="op-btn danger" type="button" @click="removeCover">移除封面</button>
            <div class="hint">建议 16:9，≤10MB，上传后自动压缩<br />换图/移除后记得点保存，旧图会一并清理</div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFile" />
        </div>
      </div>
    </div>

    <div v-if="!isNew" class="card glass" style="display:flex;justify-content:flex-end;">
      <button class="op-btn danger" type="button" :disabled="saving" @click="showDelete = true">删除这篇文章</button>
    </div>

    <Modal v-model="showDelete" title="删除文章" confirm-text="删除" @confirm="onDelete">
      <p style="line-height:1.8;font-size:14px;">
        确定删除《{{ title }}》吗？<br />
        <span class="dim">文章将被物理删除，封面图片一并清理，不可恢复。</span>
      </p>
    </Modal>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminShell from '@/components/AdminShell.vue'
import Modal from '@/components/Modal.vue'
import {
  adminGetArticle, adminCreateArticle, adminUpdateArticle,
  adminPublishArticle, adminUnpublishArticle, adminDeleteArticle, uploadImage
} from '@/api'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()

const id = computed(() => (route.params.id ? Number(route.params.id) : null))
const isNew = computed(() => !id.value)

const title = ref('')
const content = ref('')
const cover = ref(null) // { key, url }：key 原样回传 = 封面不变
const status = ref(0)
const saving = ref(false)
const uploading = ref(false)
const showDelete = ref(false)
const fileInput = ref(null)

async function load() {
  if (isNew.value) return
  try {
    const d = await adminGetArticle(id.value)
    // 归属权（2026-10-27）：非己文且非超管 → 只读，禁止进编辑
    if (d.editable === false) {
      toast('只能操作自己创建的文章')
      return router.replace('/articles')
    }
    title.value = d.title
    content.value = d.content
    status.value = d.status
    cover.value = d.coverObjectKey ? { key: d.coverObjectKey, url: d.coverUrl } : null
  } catch (e) {
    toast(e.message)
    router.replace('/articles')
  }
}

function pickFile() {
  fileInput.value?.click()
}

async function onFile(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 同一文件可重选
  if (!file) return
  if (!file.type.startsWith('image/')) return toast('只能上传图片')
  if (file.size > 10 * 1024 * 1024) return toast('图片不能超过 10MB')
  uploading.value = true
  try {
    const up = await uploadImage(file)
    // 新键（tmp 临时键）：保存时后端归档到文章名下并清旧封面
    cover.value = { key: up.objectKey, url: up.previewUrl }
    toast('封面已上传，记得保存')
  } catch (err) {
    toast(err.message)
  } finally {
    uploading.value = false
  }
}

function removeCover() {
  cover.value = null
  toast('封面已移除，保存后生效')
}

/** 保存（新建=存草稿；编辑=整体替换式更新）→ 返回文章 id */
async function save() {
  const t = title.value.trim()
  if (!t) { toast('标题必填'); return null }
  if (t.length > 120) { toast('标题不能超过 120 字'); return null }
  if (!content.value.trim()) { toast('正文必填'); return null }

  saving.value = true
  try {
    const payload = {
      title: t,
      content: content.value,
      coverObjectKey: cover.value ? cover.value.key : ''
    }
    if (isNew.value) {
      const { id: newId } = await adminCreateArticle(payload)
      toast('已存为草稿')
      return newId
    }
    await adminUpdateArticle(id.value, payload)
    toast('已保存')
    return id.value
  } catch (e) {
    toast(e.message)
    return null
  } finally {
    saving.value = false
  }
}

async function onSave() {
  const ok = await save()
  if (ok) router.push('/articles') // 保存后回到文章管理列表
}

async function onSavePublish() {
  const savedId = await save()
  if (!savedId) return
  try {
    await adminPublishArticle(savedId)
    toast('已发布，普通用户现在可见')
    router.push('/articles') // 发布后回到文章管理列表
  } catch (e) {
    toast(e.message)
  }
}

async function onUnpublish() {
  saving.value = true
  try {
    await adminUnpublishArticle(id.value)
    toast('已下线（保留为草稿，可再次发布）')
    router.push('/articles') // 下线后回到文章管理列表
  } catch (e) {
    toast(e.message)
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  saving.value = true
  try {
    await adminDeleteArticle(id.value)
    toast('文章已删除')
    router.replace('/articles')
  } catch (e) {
    toast(e.message)
  } finally {
    saving.value = false
    showDelete.value = false
  }
}

onMounted(load)
</script>
