// 极简全局提示（不用组件库）
import { reactive } from 'vue'

const state = reactive({ show: false, text: '' })
let timer = null

export function useToast() {
  return state
}

export function toast(text, ms = 2200) {
  state.text = text
  state.show = true
  clearTimeout(timer)
  timer = setTimeout(() => { state.show = false }, ms)
}
