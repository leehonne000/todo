import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { join, resolve, sep } from 'node:path'

// 壁纸池在仓库根 pics/background（与 web-user 同款按需服务，不进 public）
const WALLPAPER_ROOT = resolve(fileURLToPath(new URL('../pics/background', import.meta.url)))

const MIME = { '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp' }

// 开发/预览都挂 /wallpapers/* → ../pics/background/*（防目录穿越）
function wallpaperServer() {
  return {
    name: 'wallpaper-server',
    configureServer(server) { server.middlewares.use(handler) },
    configurePreviewServer(server) { server.middlewares.use(handler) }
  }
  function handler(req, res, next) {
    if (!req.url || !req.url.startsWith('/wallpapers/')) return next()
    const rel = decodeURIComponent(req.url.slice('/wallpapers/'.length).split('?')[0])
    const file = join(WALLPAPER_ROOT, rel)
    if (!file.startsWith(WALLPAPER_ROOT + sep)) return next()
    if (!existsSync(file) || !statSync(file).isFile()) return next()
    res.setHeader('Content-Type', MIME[file.slice(file.lastIndexOf('.')).toLowerCase()] || 'application/octet-stream')
    res.setHeader('Cache-Control', 'public, max-age=86400')
    createReadStream(file).pipe(res)
  }
}

// 开发配置说明：
// - 默认 mock 模式（VITE_API_MODE 不设或 =mock），全部数据走前端假数据，不依赖后端
// - 联调时启动后端（8080）并用 VITE_API_MODE=real npm run dev，/api 会被代理到后端
// - 端口 5174 与 web-user（5173）错开，两个工程可同时开
export default defineConfig({
  // 生产构建挂 /admin/ 子路径（与 web-user 同域部署：用户端在 /，后台在 /admin/）；
  // 开发保持根路径不变（NODE_ENV=development），跨端跳转 DEV 分支指 5174 根
  base: process.env.NODE_ENV === 'production' ? '/admin/' : '/',
  plugins: [vue(), wallpaperServer()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
