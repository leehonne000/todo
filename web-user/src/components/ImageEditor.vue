<template>
  <Teleport to="body">
    <div v-if="show" class="ie-mask" @click.self="close">
      <div class="ie-panel glass-strong">
        <div class="ie-title">编辑图片</div>

        <!-- 画布预览 -->
        <div class="ie-canvas-wrap">
          <canvas ref="cv" class="ie-canvas" :class="{ drawing: mosaicOn }"
                  @pointerdown="onDown" @pointermove="onMove" @pointerup="onUp" @pointerleave="onUp"></canvas>
          <div v-if="loading" class="ie-loading dim">加载中…</div>
        </div>

        <!-- 基础效果 -->
        <div class="ie-row">
          <button class="ie-btn" @click="rotate">⟳ 旋转</button>
          <span class="ie-grow"></span>
          <span class="ie-label dim">滤镜</span>
        </div>
        <TabRow :items="presetItems" v-model="preset" />

        <div class="ie-sliders">
          <label class="ie-slider">
            <span class="dim">亮度</span>
            <input type="range" min="50" max="150" v-model.number="brightnessPct" />
          </label>
          <label class="ie-slider">
            <span class="dim">对比度</span>
            <input type="range" min="50" max="150" v-model.number="contrastPct" />
          </label>
        </div>

        <!-- 展开：更多效果 -->
        <button class="ie-expand" @click="expanded = !expanded">
          {{ expanded ? '收起 ▲' : '更多效果 ▼' }}
        </button>

        <div v-if="expanded" class="ie-more">
          <div class="ie-sliders">
            <label class="ie-slider">
              <span class="dim">饱和度</span>
              <input type="range" min="0" max="200" v-model.number="saturatePct" />
            </label>
            <label class="ie-slider">
              <span class="dim">色温</span>
              <input type="range" min="-50" max="50" v-model.number="warm" />
            </label>
          </div>
          <div class="ie-row">
            <button class="ie-btn" :class="{ on: mosaicOn }" @click="mosaicOn = !mosaicOn">
              ✏️ 马赛克画笔
            </button>
            <button class="ie-btn" :disabled="!strokes.length" @click="undoMosaic">撤销一笔</button>
            <button class="ie-btn" :disabled="!strokes.length" @click="clearMosaic">清除</button>
          </div>
          <div class="ie-hint dim">打开画笔后在图上涂抹即可打码（如遮住答案）</div>
        </div>

        <!-- 操作 -->
        <div class="ie-actions">
          <button class="ie-btn" @click="reset">重置</button>
          <button class="ie-btn" @click="close">取消</button>
          <button class="btn-amber ie-apply" :disabled="loading" @click="apply">应用</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import TabRow from '@/components/TabRow.vue'
import { toast } from '@/utils/toast'

/**
 * 图片编辑器：基础（旋转/滤镜/亮度/对比度）+ 展开更多（饱和度/色温/马赛克画笔）。
 * 编辑在本地画布完成，「应用」产出 JPEG blob 交调用方重新上传。
 */
const props = defineProps({
  show: { type: Boolean, default: false },
  /** 图片地址：优先本地 blob URL（无跨域问题） */
  src: { type: String, default: '' }
})
const emit = defineEmits(['update:show', 'apply'])

const cv = ref(null)
const loading = ref(false)
const expanded = ref(false)
const mosaicOn = ref(false)

const rotation = ref(0)
const preset = ref('none')
const brightnessPct = ref(100)
const contrastPct = ref(100)
const saturatePct = ref(100)
const warm = ref(0)
const strokes = ref([]) // [{ size, points: [{x,y}] }]

const presetItems = [
  { value: 'none', label: '原图' },
  { value: 'enhance', label: '增强' },
  { value: 'gray', label: '黑白' },
  { value: 'scan', label: '扫描' }
]

let img = null
let drawing = false
let currentStroke = null

// 预设 → 滑杆值（拍题场景：增强=提亮加对比；扫描=文档二值化感）
watch(preset, (p) => {
  const table = {
    none: [100, 100, 100, 0],
    enhance: [108, 118, 112, 0],
    gray: [100, 105, 0, 0],
    scan: [112, 145, 0, 0]
  }
  const [b, c, s, w] = table[p] || table.none
  brightnessPct.value = b
  contrastPct.value = c
  saturatePct.value = s
  warm.value = w
})

// 任何参数变化重绘
watch([rotation, brightnessPct, contrastPct, saturatePct, warm, strokes], () => render(), { deep: true })

watch(() => props.show, async (v) => {
  if (!v) return
  loading.value = true
  try {
    await loadImage(props.src)
    reset()
  } catch {
    toast('图片加载失败，无法编辑')
    close()
  } finally {
    loading.value = false
  }
})

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const im = new Image()
    im.crossOrigin = 'anonymous'
    im.onload = () => { img = im; resolve() }
    im.onerror = () => reject(new Error('load failed'))
    im.src = src
  })
}

const filterCss = computed(() => {
  const b = brightnessPct.value / 100
  const c = contrastPct.value / 100
  const s = saturatePct.value / 100
  const w = warm.value / 50 // -1 冷 .. 1 暖
  const sep = w > 0 ? (w * 0.35).toFixed(2) : 0
  const hue = w < 0 ? Math.round(180 + w * 40) : 0
  return `brightness(${b}) contrast(${c}) saturate(${s}) sepia(${sep}) hue-rotate(${hue}deg)`
})

function render() {
  if (!img || !cv.value) return
  const rot = ((rotation.value % 360) + 360) % 360
  const swap = rot === 90 || rot === 270
  const w = swap ? img.height : img.width
  const h = swap ? img.width : img.height
  const canvas = cv.value
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  ctx.save()
  ctx.filter = filterCss.value
  ctx.translate(w / 2, h / 2)
  ctx.rotate((rot * Math.PI) / 180)
  ctx.drawImage(img, -img.width / 2, -img.height / 2)
  ctx.restore()
  ctx.filter = 'none'
  drawMosaic(ctx)
}

/** 马赛克：小格化再放大贴回（imageSmoothing=false 出马赛克块） */
function drawMosaic(ctx) {
  for (const stroke of strokes.value) {
    for (const p of stroke.points) {
      const s = stroke.size
      const x = Math.max(0, p.x - s / 2)
      const y = Math.max(0, p.y - s / 2)
      const cells = Math.max(2, Math.round(s / 8))
      const tmp = document.createElement('canvas')
      tmp.width = cells
      tmp.height = cells
      const tctx = tmp.getContext('2d')
      tctx.drawImage(cv.value, x, y, s, s, 0, 0, cells, cells)
      ctx.imageSmoothingEnabled = false
      ctx.drawImage(tmp, 0, 0, cells, cells, x, y, s, s)
      ctx.imageSmoothingEnabled = true
    }
  }
}

function toCanvasPos(e) {
  const rect = cv.value.getBoundingClientRect()
  return {
    x: (e.clientX - rect.left) * (cv.value.width / rect.width),
    y: (e.clientY - rect.top) * (cv.value.height / rect.height)
  }
}

function onDown(e) {
  if (!mosaicOn.value) return
  drawing = true
  cv.value.setPointerCapture?.(e.pointerId)
  const size = Math.max(18, Math.round(cv.value.width / 22))
  currentStroke = { size, points: [toCanvasPos(e)] }
  strokes.value.push(currentStroke)
}

function onMove(e) {
  if (!drawing || !currentStroke) return
  currentStroke.points.push(toCanvasPos(e))
  render()
}

function onUp() {
  drawing = false
  currentStroke = null
}

function rotate() {
  rotation.value = (rotation.value + 90) % 360
}

function undoMosaic() {
  strokes.value.pop()
}

function clearMosaic() {
  strokes.value = []
}

function reset() {
  rotation.value = 0
  preset.value = 'none'
  brightnessPct.value = 100
  contrastPct.value = 100
  saturatePct.value = 100
  warm.value = 0
  strokes.value = []
  mosaicOn.value = false
  render()
}

function apply() {
  render()
  cv.value.toBlob((blob) => {
    if (!blob) return toast('导出失败，请重试')
    emit('apply', blob)
    close()
  }, 'image/jpeg', 0.92)
}

function close() {
  emit('update:show', false)
}
</script>

<style scoped>
.ie-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 16px;
}
.ie-panel {
  width: 100%;
  max-width: 520px;
  max-height: 88vh;
  overflow-y: auto;
  border-radius: 18px;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
}
.ie-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.ie-canvas-wrap {
  position: relative;
  width: 100%;
  max-height: 38vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 12px;
  overflow: hidden;
}
.ie-canvas {
  max-width: 100%;
  max-height: 38vh;
  display: block;
  touch-action: none;
}
.ie-canvas.drawing { cursor: crosshair; }
.ie-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ie-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}
.ie-grow { flex: 1; }
.ie-label { font-size: 13px; }
.ie-btn {
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  color: var(--text-dim);
  border: 1px solid rgba(255, 255, 255, 0.18);
}
.ie-btn.on {
  color: var(--theme-text);
  border-color: var(--theme-text);
}
.ie-btn:disabled { opacity: 0.4; }
.ie-sliders {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}
.ie-slider {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}
.ie-slider span { width: 44px; flex-shrink: 0; }
.ie-slider input[type='range'] { flex: 1; accent-color: var(--theme); }
.ie-expand {
  width: 100%;
  margin-top: 14px;
  padding: 9px;
  border-radius: 10px;
  font-size: 13px;
  color: var(--text-dim);
  border: 1px dashed rgba(255, 255, 255, 0.22);
}
.ie-more { margin-top: 12px; }
.ie-hint { margin-top: 8px; font-size: 12px; line-height: 1.6; }
.ie-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}
.ie-actions .ie-btn { flex: 1; padding: 12px; }
.ie-apply { flex: 2; padding: 12px; }
</style>
