// 跨端口共享偏好存储（cookie 版 + 时间戳仲裁）
// 为什么不用 localStorage：它按端口隔离（用户端 5173 / 管理后台 5174 各存各的），
// 用户端换的壁纸进管理后台就读不到；cookie 按域名共享、不分端口，两端读到同一份。
// 仲裁（2026-10-27 二次修复）：cookie 与 localStorage 不一致时取 updatedAt 较新的一方——
// 旧数据无时间戳视为 0，防"哪端先加载哪端的旧值种子进 cookie"的竞态。
const YEAR = 365 * 24 * 60 * 60

function readCookie(key) {
  try {
    const esc = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const m = document.cookie.match('(?:^|; )' + esc + '=([^;]*)')
    return m ? decodeURIComponent(m[1]) : null
  } catch { return null }
}

function readLocal(key) {
  try { return localStorage.getItem(key) } catch { return null }
}

function tsOf(raw) {
  try { return JSON.parse(raw)?.updatedAt || 0 } catch { return 0 }
}

export function readPref(key) {
  const c = readCookie(key)
  const l = readLocal(key)
  if (c != null && l != null && c !== l) {
    const win = tsOf(l) > tsOf(c) ? l : c
    writePref(key, win) // 仲裁结果同步两边
    return win
  }
  const v = c ?? l
  if (c == null && l != null) writePref(key, v) // 自愈迁移：种子进 cookie
  return v
}

export function writePref(key, value) {
  try {
    document.cookie = key + '=' + encodeURIComponent(value) + '; path=/; max-age=' + YEAR + '; SameSite=Lax'
  } catch { /* 忽略 */ }
  try { localStorage.setItem(key, value) } catch { /* 忽略 */ }
}
