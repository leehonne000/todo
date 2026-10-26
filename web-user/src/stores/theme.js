// 主题色：全局换肤（琥珀 → 任意色，含 GitHub 贡献墙绿）
// 原理：把主题色写进 :root 的 CSS 变量，各组件只认变量不认死色值
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const KEY = 'exam-timeline-theme'

// 7 个经典配色（含 GitHub 贡献墙绿）。deep = 加深版（实心按钮用）
export const PRESETS = [
  { name: '琥珀', main: '#fac775', deep: '#ef9f27' },
  { name: 'GitHub 绿', main: '#2da44e', deep: '#1a7f37' },
  { name: '天蓝', main: '#85b7eb', deep: '#378add' },
  { name: '珊瑚', main: '#f0997b', deep: '#d85a30' },
  { name: '青绿', main: '#5dcaa5', deep: '#1d9e75' },
  { name: '紫罗兰', main: '#afa9ec', deep: '#7f77dd' },
  { name: '玫粉', main: '#ed93b1', deep: '#d4537e' }
]

// 默认主题色 = 标准白（2026-10-26 用户指定）
const DEFAULT = { name: '默认白', main: '#ffffff', deep: '#bfbfbf' }

// ---------- 颜色小工具 ----------
function parseHex(hex) {
  const h = hex.replace('#', '')
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16)
  ]
}

/** 任意主色 → 加深版（HSL 明度 × 0.75），自定义色盘用 */
export function deriveDeep(hex) {
  let [r, g, b] = parseHex(hex)
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b)
  let h = 0, s = 0
  const l = (max + min) / 2
  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6
    else if (max === g) h = ((b - r) / d + 2) / 6
    else h = ((r - g) / d + 4) / 6
  }
  const l2 = Math.max(0.12, l * 0.75)
  // HSL → RGB
  const a = s * Math.min(l2, 1 - l2)
  const f = (n) => {
    const k = (n + h * 12) % 12
    const c = l2 - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)))
    return Math.round(255 * c)
  }
  return '#' + [f(0), f(8), f(4)].map(v => v.toString(16).padStart(2, '0')).join('')
}

function applyToRoot(main, deep) {
  const [r, g, b] = parseHex(main)
  const [dr, dg, db] = parseHex(deep)
  // 主题色偏暗时按钮文字用白色，偏亮时用深棕（保证实心按钮上的字始终可读）
  const lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  const on = lum < 0.5 ? '#ffffff' : '#2a1c05'
  // 「可读墨色」：深色主题在深底上会隐形（黑主题黑字），向白提亮到可读；
  // 浅色主题直接用原色。所有文字/下划线/热力格用它。
  const mix = 0.62
  const tr = Math.round(r + (255 - r) * (lum < 0.5 ? mix : 0))
  const tg = Math.round(g + (255 - g) * (lum < 0.5 ? mix : 0))
  const tb = Math.round(b + (255 - b) * (lum < 0.5 ? mix : 0))
  const s = document.documentElement.style
  s.setProperty('--theme', main)
  s.setProperty('--theme-rgb', `${r} ${g} ${b}`)
  s.setProperty('--theme-deep', deep)
  s.setProperty('--theme-deep-rgb', `${dr} ${dg} ${db}`)
  s.setProperty('--theme-on', on)
  s.setProperty('--theme-text', `rgb(${tr} ${tg} ${tb})`)
  s.setProperty('--theme-text-rgb', `${tr} ${tg} ${tb}`)
}

export const useThemeStore = defineStore('theme', () => {
  let saved = null
  try { saved = JSON.parse(localStorage.getItem(KEY)) } catch { /* 用默认 */ }

  const main = ref(saved?.main || DEFAULT.main)
  const deep = ref(saved?.deep || DEFAULT.deep)

  const isCustom = computed(() =>
    !PRESETS.some(p => p.main.toLowerCase() === main.value.toLowerCase()) &&
    main.value !== DEFAULT.main
  )

  // 初始应用（store 一被使用就生效）
  applyToRoot(main.value, deep.value)

  /** 设置主题色（预设直接带 deep；自定义色自动算 deep） */
  function setTheme(mainHex, deepHex) {
    main.value = mainHex.toLowerCase()
    deep.value = (deepHex || deriveDeep(mainHex)).toLowerCase()
    applyToRoot(main.value, deep.value)
    localStorage.setItem(KEY, JSON.stringify({ main: main.value, deep: deep.value }))
  }

  function resetDefault() {
    setTheme(DEFAULT.main, DEFAULT.deep)
  }

  return { main, deep, isCustom, setTheme, resetDefault }
})
