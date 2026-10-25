import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { join, resolve, sep } from 'node:path'

// 壁纸池在仓库根 pics/background（839MB 不进 public，按需服务）
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
export default defineConfig({
  plugins: [vue(), wallpaperServer()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
