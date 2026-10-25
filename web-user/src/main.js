import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/base.css'

// —— 一次性全局数据重置（2026-10-25）——
// 演示数据在浏览器 localStorage 里，代码侧无法主动删；此处开闸清一次：
// 清空登录态/主题/壁纸偏好 + 旧版 mock 数据，演示数据由 mock v3 重新播种。
// 标记落位后本段自动失效，之后正常使用不再触发。
const RESET_MARK = 'exam-timeline-reset-20261025-9'
if (localStorage.getItem(RESET_MARK) !== '1') {
  for (const k of [
    'exam-timeline-auth',
    'exam-timeline-theme',
    'exam-timeline-wallpaper',
    'exam-timeline-mock-db',
    'exam-timeline-mock-db-v2'
  ]) localStorage.removeItem(k)
  localStorage.setItem(RESET_MARK, '1')
}

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

