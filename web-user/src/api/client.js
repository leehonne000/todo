// API 客户端：统一处理 Result 包裹、token、错误（真实请求走 utils/request.js 的 axios 封装）
// USE_MOCK=true 走前端假数据（mock 兜底，课程第 14 课 K14-5）；联调时设 VITE_API_MODE=real
import request, { ApiError } from '@/utils/request'
import { mockRequest } from './mock'
import { useAuthStore } from '@/stores/auth'

const USE_MOCK = import.meta.env.VITE_API_MODE !== 'real'

export { ApiError }

/**
 * 业务请求。契约（接口文档 §1.2/§1.3）：
 * - 业务失败时 HTTP 仍 200，前端以 code 判断成败
 * - code=200 成功；非 200 抛 ApiError
 */
async function apiRequest(method, path, { body, query, formData } = {}) {
  if (USE_MOCK) {
    const auth = useAuthStore()
    const headers = {}
    if (auth.token) headers.Authorization = `Bearer ${auth.token}`
    const resBody = await mockRequest(
      method, path, formData ? { file: formData.get('file') } : body, query || {}, headers
    )
    if (resBody.code !== 200) {
      // 2001 = 凭据错误 → 登录态失效，踢回登录页
      if (resBody.code === 2001 && auth.token) {
        auth.logout()
        if (location.pathname !== '/login') location.href = '/login'
      }
      throw new ApiError(resBody.code, resBody.message || '请求失败')
    }
    return resBody.data
  }

  // 真实后端：axios 实例已带 baseURL=/api，这里去掉路径里的 /api 前缀防重复；
  // 响应拦截器负责解包 Result（code=200→data）与 401 处理
  const url = path.replace(/^\/api/, '') || '/'
  const config = { method, url, params: query }
  if (formData) {
    config.data = formData
  } else if (body !== undefined) {
    config.data = body
  }
  return request(config)
}

export const get = (path, query) => apiRequest('GET', path, { query })
export const post = (path, body) => apiRequest('POST', path, { body })
export const put = (path, body) => apiRequest('PUT', path, { body })
export const del = (path) => apiRequest('DELETE', path)

// multipart 上传（接口文档 §5.5）
export function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return apiRequest('POST', '/api/uploads/images', { formData: fd })
}

export { USE_MOCK }
