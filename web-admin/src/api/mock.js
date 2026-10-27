// ============================================================
// Mock 数据层 —— 严格对齐 docs/接口文档.md §11 的响应契约
// 后端联调完成后，把 api/client.js 的 VITE_API_MODE 设为 real 即可切换
// 角色三级制（2026-10-27）：0=普通用户 1=管理员 2=超级管理员
// 演示超管：admin@bigtree.com / <VITE_SUPER_ADMIN_PASSWORD>（role=2，全权；密码在 .env，不入库）
// 演示管理员：writer@bigtree.com / admin123456（role=1，只能动自己的文章）
// 演示普通用户：demo@kaoyan.com / demo123456（role=0，用于验证 2002 权限卡口）
// ============================================================

const STORE_KEY = 'exam-admin-mock-db-v2' // 版本号一升，旧数据作废、下次打开自动重种

function uid() {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4)
}

function now() {
  return new Date().toISOString().slice(0, 19)
}

function dateStr(offsetDays = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return d.toISOString().slice(0, 10)
}

// ---------- 种子数据 ----------
function seed() {
  // 硬节点（与 web-user mock 同款参考值）：届数前一年 12 月初试
  const milestones = []
  let msId = 1
  for (const examYear of [2027, 2028]) {
    const y = examYear - 1
    const rows = [
      ['预报名', `${y}-09-24`, `${y}-09-27`, 1],
      ['正式报名', `${y}-10-15`, `${y}-10-28`, 2],
      ['网上确认', `${y}-11-02`, `${y}-11-08`, 3],
      ['准考证下载', `${y}-12-14`, `${y}-12-20`, 4],
      ['初试', `${y}-12-19`, `${y}-12-20`, 5],
      ['出分', `${examYear}-02-20`, `${examYear}-02-28`, 6],
      ['国家线', `${examYear}-03-15`, `${examYear}-03-20`, 7],
      ['复试 / 调剂', `${examYear}-03-25`, `${examYear}-04-30`, 8]
    ]
    for (const [name, dateFrom, dateTo, sortOrder] of rows) {
      milestones.push({ id: msId++, examYear, name, dateFrom, dateTo, sortOrder })
    }
  }

  return {
    users: [
      { id: 1, email: 'admin@bigtree.com', password: import.meta.env?.VITE_SUPER_ADMIN_PASSWORD || 'admin123456', nickname: '园丁', role: 2, examYear: 2027, createdAt: `${dateStr(-320)}T09:00:00` },
      { id: 6, email: 'writer@bigtree.com', password: 'admin123456', nickname: '小笔', role: 1, examYear: 2027, createdAt: `${dateStr(-300)}T11:00:00` },
      { id: 2, email: 'demo@kaoyan.com', password: 'demo123456', nickname: 'demo', role: 0, examYear: 2027, createdAt: `${dateStr(-260)}T10:20:00` },
      { id: 3, email: 'linhua@qq.com', password: 'test123456', nickname: '林华', role: 0, examYear: 2028, createdAt: `${dateStr(-180)}T14:05:00` },
      { id: 4, email: 'xiaowang@163.com', password: 'test123456', nickname: '小王', role: 0, examYear: 2027, createdAt: `${dateStr(-90)}T21:33:00` },
      { id: 5, email: 'yiran@gmail.com', password: 'test123456', nickname: '依然', role: 0, examYear: 2028, createdAt: `${dateStr(-12)}T08:41:00` }
    ],
    tokens: {},
    articles: [
      {
        id: 1, authorId: 1,
        title: '2027 考研预报名已开启，这些材料提前备好',
        content: '预报名一般面向应届生，往届生也可参加。请提前准备：身份证、学历学籍信息、户籍与档案所在地、报考点选择。\n\n注意三点：\n1. 报名号生成后牢记，网上确认时要用；\n2. 报考点名额有限，热门城市可能满员，尽量早报；\n3. 生成报名号但未缴费视为报名无效。',
        coverObjectKey: null, coverUrl: null,
        status: 1, publishedAt: `${dateStr(-4)}T10:00:00`, updatedAt: `${dateStr(-4)}T10:00:00`
      },
      {
        id: 2, authorId: 6,
        title: '冲刺期每日时间表参考（供参考，别硬抄）',
        content: '很多同学问“一天学多久才够”。答案是：比昨天多一点就够。\n\n一个可执行的骨架：\n· 上午 3h：数学/专业课硬骨头（脑子最清醒）\n· 下午 2.5h：英语阅读/翻译或专业课\n· 晚上 2h：政治选择/错题复盘\n· 睡前 0.5h：翻今天记的 note（正好走一遍艾宾浩斯）',
        coverObjectKey: 'user_1/article_2/demo-cover.png', coverUrl: '/wallpaper.png',
        status: 1, publishedAt: `${dateStr(-8)}T20:00:00`, updatedAt: `${dateStr(-8)}T20:00:00`
      },
      {
        id: 3, authorId: 1,
        title: '关于心态：焦虑是正常的',
        content: '考研是场持久战。你不是一个人在焦虑。\n\n几个小建议：\n· 和昨天的自己比，不和别人比；\n· 累了就休息，休息不是犯罪；\n· 记录本身就是一种整理焦虑的方式。\n\n祝顺利。',
        coverObjectKey: null, coverUrl: null,
        status: 1, publishedAt: `${dateStr(-15)}T09:00:00`, updatedAt: `${dateStr(-15)}T09:00:00`
      },
      {
        id: 4, authorId: 6,
        title: '（草稿）12 月准考证打印提醒',
        content: '这是一篇草稿，普通用户看不到。\n\n打印时间：12 月 14 日起。\n多打几份，复试还要用。',
        coverObjectKey: null, coverUrl: null,
        status: 0, publishedAt: null, updatedAt: `${dateStr(-1)}T15:00:00`
      },
      {
        id: 5, authorId: 1,
        title: '（草稿）考前一周注意事项',
        content: '考前一周不学新东西，只做三件事：\n1. 过一遍错题本；\n2. 按考试时间做一套真题找节奏；\n3. 调作息。',
        coverObjectKey: null, coverUrl: null,
        status: 0, publishedAt: null, updatedAt: now()
      }
    ],
    milestones,
    nextArticleId: 6,
    nextMilestoneId: msId
  }
}

// ---------- 持久化 ----------
let db = null
function loadDb() {
  if (db) return db
  try {
    const raw = localStorage.getItem(STORE_KEY)
    if (raw) { db = JSON.parse(raw); return db }
  } catch { /* 坏数据则重种 */ }
  db = seed()
  saveDb()
  return db
}
function saveDb() {
  try { localStorage.setItem(STORE_KEY, JSON.stringify(db)) } catch { /* 存储满就算了 */ }
}

// 跨端票据登录（2026-10-27 方案 A）：把用户端带过来的票据身份登记进 mock 会话表
// （token→userId），让 mock 鉴权（currentUser）能认这个身份；库里没有该用户则补一行。
export function mockAdoptSession({ token, userId, email, role }) {
  if (!token || !userId) return
  const d = loadDb()
  d.tokens[token] = userId
  if (!d.users.find(u => u.id === userId)) {
    d.users.push({
      id: userId,
      email: email || `user${userId}@bigtree.com`,
      nickname: (email && email.split('@')[0]) || `用户${userId}`,
      role: role ?? 0,
      examYear: 2027,
      createdAt: now()
    })
  }
  saveDb()
}

// ---------- 鉴权辅助 ----------
function currentUser(headers = {}) {
  const auth = headers.Authorization || headers.authorization || ''
  const token = auth.replace('Bearer ', '').trim()
  if (!token) return null
  const d = loadDb()
  const userId = d.tokens[token]
  if (!userId) return null
  return d.users.find(u => u.id === userId) || null
}

/** 管理后台入口卡口（§11.2）：普通用户一律 2002；管理员（1）与超管（2）都可进 */
function needAdmin(user) {
  if (!user) return fail(2001, '未登录或登录已过期')
  if (user.role < 1) return fail(2002, '需要管理员权限')
  return null
}

/** 超管卡口：用户任免与硬节点维护仅超级管理员（role=2） */
function needSuperAdmin(user) {
  const denied = needAdmin(user)
  if (denied) return denied
  if (user.role < 2) return fail(2002, '需要超级管理员权限')
  return null
}

/** 文章操作权：作者本人或超级管理员；其他管理员 2002（互不干涉） */
function needArticleOperator(user, a) {
  const denied = needAdmin(user)
  if (denied) return denied
  if (user.role < 2 && a.authorId !== user.id) return fail(2002, '只能操作自己创建的文章')
  return null
}

// ---------- 业务工具 ----------
function authorNameOf(d, authorId) {
  const u = d.users.find(x => x.id === authorId)
  return u ? (u.nickname || u.email.split('@')[0]) : '未知'
}
function articleListItem(a, viewer) {
  const d = loadDb()
  return {
    id: a.id, title: a.title, status: a.status,
    authorName: authorNameOf(d, a.authorId), authorId: a.authorId,
    publishedAt: a.publishedAt, updatedAt: a.updatedAt
  }
}
function articleDetail(a, viewer) {
  const d = loadDb()
  return {
    id: a.id, title: a.title, content: a.content,
    coverObjectKey: a.coverObjectKey, coverUrl: a.coverUrl,
    status: a.status,
    authorName: authorNameOf(d, a.authorId), authorId: a.authorId,
    editable: viewer.role >= 2 || a.authorId === viewer.id,
    publishedAt: a.publishedAt, updatedAt: a.updatedAt
  }
}
function userListItem(u) {
  return { id: u.id, email: u.email, nickname: u.nickname, role: u.role, examYear: u.examYear, createdAt: u.createdAt }
}
function paginate(list, q) {
  const page = Math.max(1, Number(q.page) || 1)
  const size = Math.min(50, Math.max(1, Number(q.size) || 10))
  return {
    items: list.slice((page - 1) * size, page * size),
    total: list.length, page, size
  }
}
function validateArticle(body) {
  const title = (body.title || '').trim()
  if (!title) return '标题必填'
  if (title.length > 120) return '标题不能超过 120 字'
  if (!body.content || !String(body.content).trim()) return '正文必填'
  return null
}
function validateMilestone(body) {
  const y = Number(body.examYear)
  if (!y || y < 2000 || y > 2100) return '届数 examYear 需在 2000~2100'
  if (!(body.name || '').trim()) return '节点名必填'
  if (body.name.trim().length > 32) return '节点名不能超过 32 字'
  if (!body.dateFrom) return '起始日 dateFrom 必填'
  if (body.dateTo && body.dateTo < body.dateFrom) return '结束日不能早于起始日'
  return null
}

// ---------- 路由表 ----------
const routes = {
  // ---- §3 认证（管理后台只用登录；role 一并返回做入口门禁）----
  'POST /api/auth/login': (ctx) => {
    const { email, password } = ctx.body
    const d = loadDb()
    const u = d.users.find(x => x.email === email && x.password === password)
    if (!u) return fail(2001, '邮箱或密码错误')
    const token = 'adm-' + uid()
    d.tokens[token] = u.id
    saveDb()
    return ok({ token, userId: u.id, email: u.email, role: u.role })
  },

  // ---- §3.5 用户资料（左下角头像/个性化面板展示用）----
  'GET /api/profile': (ctx) => {
    const u = ctx.user
    if (!u) return fail(2001, '未登录')
    return ok({
      userId: u.id,
      email: u.email,
      nickname: u.nickname || (u.email || '').split('@')[0],
      role: u.role,
      avatarUrl: u.avatarUrl || '/avatar-default.png',
      examYear: u.examYear || null
    })
  },

  // ---- §5.5 上传（文章封面）----
  'POST /api/uploads/images': (ctx) => {
    const file = ctx.body.file
    if (!file) return fail(1001, '缺少文件')
    return new Promise((resolve) => {
      const reader = new FileReader()
      reader.onload = () => {
        const objectKey = `user_${ctx.user.id}/tmp/${uid()}.png`
        resolve(ok({ objectKey, contentType: file.type || 'image/png', sizeBytes: file.size, previewUrl: reader.result }))
      }
      reader.onerror = () => resolve(fail(5000, '文件读取失败'))
      reader.readAsDataURL(file)
    })
  },

  // ---- §11.1 文章列表（含草稿）----
  'GET /api/admin/articles': (ctx) => {
    const denied = needAdmin(ctx.user); if (denied) return denied
    const d = loadDb()
    let list = [...d.articles].sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
    if (ctx.query.status !== undefined && ctx.query.status !== '') {
      const st = Number(ctx.query.status)
      if (st !== 0 && st !== 1) return fail(1001, 'status 只能是 0（草稿）或 1（已发布）')
      list = list.filter(a => a.status === st)
    }
    return ok({ ...paginate(list.map(a => articleListItem(a, ctx.user)), ctx.query) })
  },

  // ---- §11.2 新建文章（存草稿）----
  'POST /api/admin/articles': (ctx) => {
    const denied = needAdmin(ctx.user); if (denied) return denied
    const err = validateArticle(ctx.body); if (err) return fail(1001, err)
    const d = loadDb()
    const a = {
      id: d.nextArticleId++,
      authorId: ctx.user.id,
      title: ctx.body.title.trim(),
      content: ctx.body.content,
      coverObjectKey: null, coverUrl: null,
      status: 0, publishedAt: null, updatedAt: now()
    }
    // 封面整体替换式（与后端一致）：空=无封面
    const k = (ctx.body.coverObjectKey || '').trim()
    if (k) { a.coverObjectKey = k.replace('/tmp/', '/article_' + a.id + '/'); a.coverUrl = ctx.body.coverUrl || null }
    d.articles.push(a)
    saveDb()
    return ok({ id: a.id })
  },

  // ---- §11.9 文章详情（编辑回显）----
  'GET /api/admin/articles/:id': (ctx) => {
    const denied = needAdmin(ctx.user); if (denied) return denied
    const a = loadDb().articles.find(x => x.id === Number(ctx.params.id))
    if (!a) return fail(3002, '文章不存在')
    return ok(articleDetail(a, ctx.user))
  },

  // ---- §11.3 编辑文章（整体替换式；仅作者或超管）----
  'PUT /api/admin/articles/:id': (ctx) => {
    const err = validateArticle(ctx.body); if (err) return fail(1001, err)
    const d = loadDb()
    const a = d.articles.find(x => x.id === Number(ctx.params.id))
    if (!a) return fail(3002, '文章不存在')
    const denied = needArticleOperator(ctx.user, a); if (denied) return denied
    a.title = ctx.body.title.trim()
    a.content = ctx.body.content
    const k = (ctx.body.coverObjectKey || '').trim()
    if (!k) {
      a.coverObjectKey = null; a.coverUrl = null // 清封面
    } else if (k !== a.coverObjectKey) {
      a.coverObjectKey = k.replace('/tmp/', '/article_' + a.id + '/') // 换封面（旧对象由后端清）
      a.coverUrl = ctx.body.coverUrl || a.coverUrl
    }
    a.updatedAt = now()
    saveDb()
    return ok(null)
  },

  // ---- §11.4 发布（仅作者或超管）----
  'POST /api/admin/articles/:id/publish': (ctx) => {
    const a = loadDb().articles.find(x => x.id === Number(ctx.params.id))
    if (!a) return fail(3002, '文章不存在')
    const denied = needArticleOperator(ctx.user, a); if (denied) return denied
    a.status = 1
    if (!a.publishedAt) a.publishedAt = now() // 首次发布写入，之后不变
    a.updatedAt = now()
    saveDb()
    return ok(null)
  },

  // ---- §11.5 下线（仅作者或超管）----
  'POST /api/admin/articles/:id/unpublish': (ctx) => {
    const a = loadDb().articles.find(x => x.id === Number(ctx.params.id))
    if (!a) return fail(3002, '文章不存在')
    const denied = needArticleOperator(ctx.user, a); if (denied) return denied
    a.status = 0
    a.updatedAt = now()
    saveDb()
    return ok(null)
  },

  // ---- §11.6 删除文章（仅作者或超管）----
  'DELETE /api/admin/articles/:id': (ctx) => {
    const d = loadDb()
    const idx = d.articles.findIndex(x => x.id === Number(ctx.params.id))
    if (idx < 0) return fail(3002, '文章不存在')
    const denied = needArticleOperator(ctx.user, d.articles[idx]); if (denied) return denied
    d.articles.splice(idx, 1)
    saveDb()
    return ok(null)
  },

  // ---- §11.7 查用户（仅超管）----
  'GET /api/admin/users': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const d = loadDb()
    let list = [...d.users].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
    const kw = (ctx.query.keyword || '').trim().toLowerCase()
    if (kw) {
      list = list.filter(u =>
        u.email.toLowerCase().includes(kw) ||
        (u.nickname || '').toLowerCase().includes(kw) ||
        String(u.id) === kw
      )
    }
    return ok({ ...paginate(list.map(userListItem), ctx.query) })
  },

  // ---- §11.8 任免管理员（仅超管；超管身份不可被任免）----
  'POST /api/admin/users/:id/role': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const role = ctx.body.role
    if (role !== 0 && role !== 1) return fail(1001, 'role 只能是 1（提拔为管理员）或 0（取消管理员）')
    const d = loadDb()
    const target = d.users.find(x => x.id === Number(ctx.params.id))
    if (!target) return fail(3002, '用户不存在')
    if (target.id === ctx.user.id) {
      return fail(3001, '不能变更自己的角色，请让部署者操作')
    }
    if (target.role >= 2) {
      return fail(3001, '超级管理员身份由部署者指定，不可在此变更')
    }
    target.role = role
    saveDb()
    return ok({ id: target.id, role: target.role })
  },

  // ---- §11.10 硬节点列表（仅超管）----
  'GET /api/admin/milestones': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const d = loadDb()
    let list = [...d.milestones]
    if (ctx.query.examYear !== undefined && ctx.query.examYear !== '') {
      list = list.filter(m => m.examYear === Number(ctx.query.examYear))
    }
    list.sort((a, b) => a.examYear - b.examYear || a.sortOrder - b.sortOrder)
    return ok({ items: list.map(m => ({ ...m })) })
  },

  // ---- §11.11 新建硬节点（仅超管）----
  'POST /api/admin/milestones': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const err = validateMilestone(ctx.body); if (err) return fail(1001, err)
    const d = loadDb()
    const m = {
      id: d.nextMilestoneId++,
      examYear: Number(ctx.body.examYear),
      name: ctx.body.name.trim(),
      dateFrom: ctx.body.dateFrom,
      dateTo: ctx.body.dateTo || null,
      sortOrder: Number(ctx.body.sortOrder) >= 0 ? Number(ctx.body.sortOrder) : 0
    }
    d.milestones.push(m)
    saveDb()
    return ok({ ...m })
  },

  // ---- §11.12 编辑硬节点（仅超管）----
  'PUT /api/admin/milestones/:id': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const err = validateMilestone(ctx.body); if (err) return fail(1001, err)
    const d = loadDb()
    const m = d.milestones.find(x => x.id === Number(ctx.params.id))
    if (!m) return fail(3002, '硬节点不存在')
    m.examYear = Number(ctx.body.examYear)
    m.name = ctx.body.name.trim()
    m.dateFrom = ctx.body.dateFrom
    m.dateTo = ctx.body.dateTo || null
    m.sortOrder = Number(ctx.body.sortOrder) >= 0 ? Number(ctx.body.sortOrder) : 0
    saveDb()
    return ok({ ...m })
  },

  // ---- §11.13 删除硬节点（仅超管）----
  'DELETE /api/admin/milestones/:id': (ctx) => {
    const denied = needSuperAdmin(ctx.user); if (denied) return denied
    const d = loadDb()
    const idx = d.milestones.findIndex(x => x.id === Number(ctx.params.id))
    if (idx < 0) return fail(3002, '硬节点不存在')
    d.milestones.splice(idx, 1)
    saveDb()
    return ok(null)
  }
}

// ---------- 响应包装（契约 §1.2/§1.3） ----------
function ok(data, message = 'ok') { return { code: 0, message, data } }
function fail(code, message) { return { code, message, data: null } }

// ---------- 对外入口 ----------
const needAuth = (path) => !path.startsWith('/api/auth/')

/**
 * 模拟一次 HTTP 请求
 * @returns {Promise<{code:number,message:string,data:any}>}
 */
export function mockRequest(method, path, body = null, query = {}, headers = {}) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const d = loadDb()
      const user = currentUser(headers)
      if (needAuth(path) && !user) return resolve(fail(2001, '未登录或登录已过期'))

      // 路径匹配（支持 :param）
      let matched = false
      for (const key of Object.keys(routes)) {
        const [m, pattern] = key.split(' ')
        if (m !== method) continue
        const paramNames = []
        const regex = new RegExp('^' + pattern.replace(/:(\w+)/g, (_, n) => { paramNames.push(n); return '([^/]+)' }) + '$')
        const hit = path.match(regex)
        if (!hit) continue
        const params = {}
        paramNames.forEach((n, i) => { params[n] = decodeURIComponent(hit[i + 1]) })
        matched = true
        try {
          resolve(routes[key]({ body: body || {}, query, params, user, db: d }))
        } catch (err) {
          resolve(fail(5000, '服务器内部错误：' + err.message))
        }
        break
      }
      if (!matched) resolve(fail(3002, '接口不存在'))
    }, 80 + Math.random() * 120) // 小延迟，模拟网络，顺便露出加载态
  })
}

/** 重置演示数据（调试用） */
export function resetMockDb() {
  localStorage.removeItem(STORE_KEY)
  db = null
}
