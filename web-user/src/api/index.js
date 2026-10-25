// 接口封装：一个接口一个函数，签名与 docs/接口文档.md 一一对应
// 页面只 import 这里，不直接摸 client/mock
import { get, post, put, del, uploadImage } from './client'

// ---- §3 认证 ----
export const sendCode = (email, purpose) => post('/api/auth/send-code', { email, purpose })
export const register = (data) => post('/api/auth/register', data) // {email,password,examYear,code,subjects[]}
export const login = (email, password) => post('/api/auth/login', { email, password })
export const resetPassword = (data) => post('/api/auth/reset-password', data) // {email,code,newPassword}

// ---- §3.5 用户资料（昵称/头像，2026-10-25 增补）----
export const getProfile = () => get('/api/profile')
export const updateProfile = (data) => put('/api/profile', data) // {nickname?, avatarObjectKey?}

// ---- §4 主界面 ----
export const getHome = () => get('/api/home')
export const checkIn = () => post('/api/check-ins')

// ---- §5 Learn ----
export const getSubjects = () => get('/api/subjects')
export const startSession = (subjectId) => post('/api/sessions', { subjectId })
export const finishSession = (id, durationSeconds) => post(`/api/sessions/${id}/finish`, { durationSeconds })
export const saveSessionEntries = (id, entries) => post(`/api/sessions/${id}/entries`, { entries })
export { uploadImage }

// ---- §6 Review ----
export const getDueReviews = () => get('/api/reviews/due')
export const completeReview = (entryId) => post(`/api/reviews/${entryId}/complete`)

// ---- §7 自由库 + 标签 ----
export const getEntries = (params) => get('/api/entries', params) // {subjectId,type,tagIds,page,size}
export const getEntry = (id) => get(`/api/entries/${id}`)
export const updateEntry = (id, data) => put(`/api/entries/${id}`, data) // {content,tagNames,imageObjectKeys}
export const deleteEntry = (id) => del(`/api/entries/${id}`)
export const getTags = (q) => get('/api/tags', { q })

// ---- §8 进度 ----
export const getProgress = (from, to) => get('/api/progress', { from, to })

// ---- §9 信息模块 ----
export const getInfoOverview = () => get('/api/info/overview')
export const getArticles = (page, size) => get('/api/articles', { page, size })
export const getArticle = (id) => get(`/api/articles/${id}`)
