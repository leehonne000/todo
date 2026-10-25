// API 客户端：统一处理 ApiResponse 包裹、token、错误
// USE_MOCK=true 走前端假数据（当前后端业务接口未实现，联调时改 false 或设 VITE_API_MODE=real）
import { mockRequest } from './mock'
import { useAuthStore } from '@/stores/auth'

const USE_MOCK = import.meta.env.VITE_API_MODE !== 'real'

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
  }
}

function buildQuery(query = {}) {
  const parts = Object.entries(query)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
  return parts.length ? '?' + parts.join('&') : ''
}

/**
 * 业务请求。契约（接口文档 §1.2/§1.3）：
 * - 业务失败时 HTTP 仍 200，前端以 code 判断成败
 * - code=0 成功；非 0 抛 ApiError
 */
async function request(method, path, { body, query, formData } = {}) {
  const auth = useAuthStore()
  const headers = {}
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`

  let resBody
  if (USE_MOCK) {
    resBody = await mockRequest(method, path, formData ? { file: formData.get('file') } : body, query || {}, headers)
  } else {
    const url = path + buildQuery(query)
    const init = { method, headers }
    if (formData) {
      init.body = formData
    } else if (body !== undefined) {
      headers['Content-Type'] = 'application/json'
      init.body = JSON.stringify(body)
    }
    const res = await fetch(url, init)
    resBody = await res.json()
  }

  if (resBody.code !== 0) {
    // 2001 = 凭据错误 → 登录态失效，踢回登录页
    if (resBody.code === 2001 && auth.token) {
      auth.logout()
      if (location.pathname !== '/login') location.href = '/login'
    }
    throw new ApiError(resBody.code, resBody.message || '请求失败')
  }
  return resBody.data
}

export const get = (path, query) => request('GET', path, { query })
export const post = (path, body) => request('POST', path, { body })
export const put = (path, body) => request('PUT', path, { body })
export const del = (path) => request('DELETE', path)

// multipart 上传（接口文档 §5.5）
export function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request('POST', '/api/uploads/images', { formData: fd })
}

export { USE_MOCK }
