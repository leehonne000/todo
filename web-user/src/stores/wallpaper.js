// 壁纸自定义：分类随机换壁纸 + 交叉过渡 + localStorage 记忆 + 每日更新开关 + 画面调节
import { defineStore } from 'pinia'
import { ref, shallowRef, computed, watch } from 'vue'
import { dateStr } from '@/utils/format'

const KEY = 'exam-timeline-wallpaper'
const DEFAULT = { src: '/wallpaper.png', category: null, fileName: 'Out on a Limb' }

/** 当前壁纸（两层交叉过渡用） */
export const useWallpaperStore = defineStore('wallpaper', () => {
  let saved = null
  try { saved = JSON.parse(localStorage.getItem(KEY)) } catch { /* 坏数据用默认 */ }

  // current 正在显示，incoming 淡入中；两层轮换实现无闪烁交叉过渡
  const current = ref(saved?.src || DEFAULT.src)
  const incoming = shallowRef('')
  const fading = ref(false)
  const category = ref(saved?.category ?? DEFAULT.category)
  const fileName = ref(saved?.fileName || DEFAULT.fileName)

  // 每日更新：开启后每天首次打开自动随机换一张（沿用当前分类）
  const dailyUpdate = ref(Boolean(saved?.dailyUpdate))
  const lastDaily = ref(saved?.lastDaily || '')

  // 画面调节（%）：壁纸明暗/对比/饱和，CSS filter 实时生效
  const brightness = ref(saved?.adjust?.brightness ?? 100)
  const contrast = ref(saved?.adjust?.contrast ?? 100)
  const saturate = ref(saved?.adjust?.saturate ?? 100)

  const filterCss = computed(() =>
    `brightness(${brightness.value}%) contrast(${contrast.value}%) saturate(${saturate.value}%)`
  )

  function persist() {
    localStorage.setItem(KEY, JSON.stringify({
      src: current.value,
      category: category.value,
      fileName: fileName.value,
      dailyUpdate: dailyUpdate.value,
      lastDaily: lastDaily.value,
      adjust: { brightness: brightness.value, contrast: contrast.value, saturate: saturate.value }
    }))
  }

  watch([brightness, contrast, saturate], persist)

  // 预加载完成才切换，避免拉大图时闪白
  function setWallpaper(src, meta = {}) {
    if (!src || src === current.value) return
    const img = new Image()
    img.onload = () => {
      incoming.value = src
      fading.value = true
      // 过渡时长与 CSS transition 对齐（600ms）
      setTimeout(() => {
        current.value = src
        incoming.value = ''
        fading.value = false
      }, 620)
      category.value = meta.category ?? category.value
      fileName.value = meta.fileName ?? fileName.value
      persist()
    }
    img.src = src
  }

  function setDailyUpdate(v) {
    dailyUpdate.value = Boolean(v)
    if (dailyUpdate.value) lastDaily.value = dateStr() // 开关当天不算换日
    persist()
  }

  /** 每日更新检查：App 启动时调用 */
  async function dailyCheck() {
    if (!dailyUpdate.value || lastDaily.value === dateStr()) return
    const cats = await loadManifest()
    if (!cats.length) return
    const cat = category.value || cats[Math.floor(Math.random() * cats.length)].name
    const picked = await randomFromCategory(cat)
    if (!picked) return
    lastDaily.value = dateStr()
    setWallpaper(picked.src, { category: cat, fileName: picked.fileName })
    persist()
  }

  /** 画面调节归 100 */
  function resetAdjust() {
    brightness.value = 100
    contrast.value = 100
    saturate.value = 100
  }

  return {
    current, incoming, fading, category, fileName,
    dailyUpdate, lastDaily,
    brightness, contrast, saturate, filterCss,
    setWallpaper, setDailyUpdate, dailyCheck, resetAdjust
  }
})

// ---------- 壁纸清单（分类 → 文件） ----------
let manifestPromise = null

/** 读取 public/wallpapers-manifest.json（构建时生成） */
export function loadManifest() {
  if (!manifestPromise) {
    manifestPromise = fetch('/wallpapers-manifest.json')
      .then(r => r.json())
      .then(m => m.categories || [])
      .catch(() => [])
  }
  return manifestPromise
}

/** 从某分类随机取一张（url 编码中文分类名），返回 { src, fileName } */
export async function randomFromCategory(categoryName) {
  const cats = await loadManifest()
  const cat = cats.find(c => c.name === categoryName)
  if (!cat || !cat.files.length) return null
  const fileName = cat.files[Math.floor(Math.random() * cat.files.length)]
  return {
    src: `/wallpapers/${encodeURIComponent(categoryName)}/${encodeURIComponent(fileName)}`,
    fileName
  }
}
