// 时间/时长格式化工具

/** 学习日起点（小时）。考研作息：04:00–次日 04:00 算一天，记在起始日 */
export const DAY_START_HOUR = 4

export function pad2(n) {
  return String(n).padStart(2, '0')
}

/** 日期 → 'YYYY-MM-DD' */
export function dateStr(d = new Date()) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

/**
 * 时刻 → 所属「学习日」（YYYY-MM-DD）。
 * 例：2026-10-26 02:00 属于 2026-10-25（因为没过 04:00）；04:00 起属于 10-26。
 * 接受 Date 或时间戳字符串。
 */
export function studyDate(d = new Date()) {
  const t = d instanceof Date ? d.getTime() : parseDate(d).getTime()
  return dateStr(new Date(t - DAY_START_HOUR * 3600 * 1000))
}

/** 时间戳字符串 → 所属学习日（studyDate 的便捷别名） */
export function dayOf(ts) {
  return studyDate(ts)
}

/** 'YYYY-MM-DD' ± n 天 → 'YYYY-MM-DD' */
export function addDaysStr(dateString, n) {
  const [y, m, d] = dateString.split('-').map(Number)
  const dt = new Date(y, m - 1, d + n)
  return dateStr(dt)
}

/** 'YYYY-MM-DDTHH:mm:ss' / 'YYYY-MM-DD' → 可读日期 '10/25 Sat.' */
export function prettyDate(s) {
  const d = parseDate(s)
  const week = ['Sun.', 'Mon.', 'Tue.', 'Wed.', 'Thu.', 'Fri.', 'Sat.']
  return `${pad2(d.getMonth() + 1)}/${pad2(d.getDate())} ${week[d.getDay()]}`
}

export function parseDate(s) {
  // Date 对象直接克隆（禁止走字符串解析：本地化时区名如“中国标准时间”会让 new Date 失败）
  if (s instanceof Date) return isNaN(s.getTime()) ? new Date() : new Date(s.getTime())
  if (!s) return new Date()
  // 兼容 '2026-10-25' 与 '2026-10-25T19:00:00'
  const d = new Date(String(s).replace('T', ' ').replace(/-/g, '/'))
  return isNaN(d.getTime()) ? new Date() : d
}

/** 秒 → 'X时Y分' / 'Y分Z秒' / 'Z秒' */
export function durationStr(totalSeconds) {
  const s = Math.max(0, Math.round(totalSeconds))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}小时${m}分`
  if (m > 0) return `${m}分${sec}秒`
  return `${sec}秒`
}

/** 秒 → 'HH:MM:SS'（计时页大数字用） */
export function clockStr(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds))
  return `${pad2(Math.floor(s / 3600))}:${pad2(Math.floor((s % 3600) / 60))}:${pad2(s % 60)}`
}

/** 当前时钟 'HH:MM:SS' */
export function nowClockStr() {
  const d = new Date()
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/** 相对「学习日今天」的天数差：负=过去（跨 04:00 边界正确归天） */
export function daysFromToday(dateString) {
  const a = parseDate(studyDate())
  const b = parseDate(dateString)
  return Math.round((b - a) / 86400000)
}
