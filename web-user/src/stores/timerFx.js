// 计时画面效果偏好（Learn 计时页左上角「圈+笔」面板设置，localStorage 持久化）
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const KEY = 'exam-timeline-timer-fx'

export const useTimerFxStore = defineStore('timerFx', () => {
  let saved = null
  try { saved = JSON.parse(localStorage.getItem(KEY)) } catch { /* 坏数据用默认 */ }

  const glow = ref(saved?.glow ?? true)         // 呼吸光晕（默认开：久待界面要有点生气）
  const particles = ref(saved?.particles ?? true) // 粒子飘动
  const pulse = ref(saved?.pulse ?? false)      // 秒数跳动
  const dim = ref(saved?.dim ?? 0)              // 背景暗度 0~80（百分比）

  watch([glow, particles, pulse, dim], () => {
    localStorage.setItem(KEY, JSON.stringify({
      glow: glow.value, particles: particles.value, pulse: pulse.value, dim: dim.value
    }))
  })

  return { glow, particles, pulse, dim }
})
