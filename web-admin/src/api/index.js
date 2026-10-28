// 接口封装：一个接口一个函数，签名与 docs/接口文档.md §11 一一对应
// 页面只 import 这里，不直接摸 client/mock
import { get, post, put, del, uploadImage } from './client'

// ---- §3 认证（共用邮箱+密码，一个账号走遍三界面）----
export const login = (email, password) => post('/api/auth/login', { email, password })
// §3.5 用户资料——跨端票据登录时用票据换服务端校验（核验 token 有效性与最新角色）
export const getProfile = () => get('/api/profile')
// §3.5 修改昵称/头像（2026-10-28：个性化页「修改昵称和头像」块）
export const updateProfile = (data) => put('/api/profile', data) // {nickname?, avatarObjectKey?}

// ---- §5.5 上传（文章封面）----
export { uploadImage }

// ---- §11.1~11.6、§11.9 文章管理 ----
export const adminListArticles = (params) => get('/api/admin/articles', params) // {status,page,size}
export const adminCreateArticle = (data) => post('/api/admin/articles', data) // {title,content,coverObjectKey?}
export const adminGetArticle = (id) => get(`/api/admin/articles/${id}`)
export const adminUpdateArticle = (id, data) => put(`/api/admin/articles/${id}`, data)
export const adminPublishArticle = (id) => post(`/api/admin/articles/${id}/publish`)
export const adminUnpublishArticle = (id) => post(`/api/admin/articles/${id}/unpublish`)
export const adminDeleteArticle = (id) => del(`/api/admin/articles/${id}`)

// ---- §11.7、§11.8 用户管理 ----
export const adminListUsers = (params) => get('/api/admin/users', params) // {keyword,page,size}
export const adminUpdateRole = (id, role) => post(`/api/admin/users/${id}/role`, { role })

// ---- §11.10~11.13 硬节点管理（增补）----
export const adminListMilestones = (examYear) => get('/api/admin/milestones', { examYear })
export const adminCreateMilestone = (data) => post('/api/admin/milestones', data)
export const adminUpdateMilestone = (id, data) => put(`/api/admin/milestones/${id}`, data)
export const adminDeleteMilestone = (id) => del(`/api/admin/milestones/${id}`)
