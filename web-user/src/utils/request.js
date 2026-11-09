// HTTP 请求封装（课程第 14 课 K14-2~K14-4：axios.create + 请求/响应拦截器）
// - 请求拦截器：自动带 Bearer Token
// - 响应拦截器：解包 Result{code,message,data}（code=200 成功返回 data）、401 登录失效跳登录页
// Content-Type 不写死：JSON 由 axios 自动带，FormData 上传走浏览器 multipart 边界
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
  }
}

const request = axios.create({
  baseURL: '/api', // 基础路径，所有请求自动拼接（vite 代理转发到后端 8080）
  timeout: 10000
})

// ---- 请求拦截器：自动加 Token ----
request.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 2001 = 凭据错误 → 登录态失效，踢回登录页 */
function kickToLogin(message) {
  const auth = useAuthStore()
  if (auth.token) {
    auth.logout()
    if (location.pathname !== '/login') location.href = '/login'
  }
  return Promise.reject(new ApiError(2001, message || '未登录或登录已过期，请先登录'))
}

// ---- 响应拦截器：解包数据、统一错误处理 ----
request.interceptors.response.use(
  (response) => {
    const res = response.data
    const hasEnvelope = res && typeof res === 'object' && 'code' in res
    if (!hasEnvelope) return res // 后端直接返回数组/对象时原样放行
    if (res.code === 200) return res.data // 成功：解包 data
    if (res.code === 2001) return kickToLogin(res.message)
    return Promise.reject(new ApiError(res.code, res.message || '业务请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      return kickToLogin(error.response?.data?.message)
    }
    return Promise.reject(error)
  }
)

export default request
