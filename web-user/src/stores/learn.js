// Learn 流程的本地状态机
// 计时/总结/编辑跨页面共享：sessionId、本次草稿条目（Done 前不落库）
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * draft 条目结构（对应接口文档 §5.4 的 entries[] 元素）：
 * { type: 1|2, content: string, tagNames: string[], imageObjectKeys: string[], imagePreviews: string[] }
 * imagePreviews 仅前端渲染缩略图用，不发给后端。
 */
export const useLearnStore = defineStore('learn', () => {
  const subjectId = ref(null)
  const subjectName = ref('')
  const sessionId = ref(null)
  const startedAt = ref('')
  const drafts = ref([])
  const editIndex = ref(-1) // -1 = 新建

  const draftCount = computed(() => drafts.value.length)

  function beginSession({ id, startedAt: st, subjectId: sid, subjectName: sname }) {
    sessionId.value = id
    startedAt.value = st
    subjectId.value = sid
    subjectName.value = sname
    drafts.value = []
    editIndex.value = -1
  }

  function addDraft(draft) {
    drafts.value.push(draft)
  }

  function updateDraft(index, draft) {
    if (index >= 0 && index < drafts.value.length) drafts.value[index] = draft
    else drafts.value.push(draft)
  }

  function reset() {
    subjectId.value = null
    subjectName.value = ''
    sessionId.value = null
    startedAt.value = ''
    drafts.value = []
    editIndex.value = -1
  }

  return { subjectId, subjectName, sessionId, startedAt, drafts, editIndex, draftCount, beginSession, addDraft, updateDraft, reset }
})
