// ============================================================
// Mock 数据层 —— 严格对齐 docs/接口文档.md 的响应契约
// 后端业务接口联调完成后，把 api/client.js 的 USE_MOCK 关掉即可切换
// 演示账号：demo@kaoyan.com / demo123456（验证码固定 123456）
// ============================================================
import { dateStr, addDaysStr, studyDate, parseDate } from '@/utils/format'

const STORE_KEY = 'exam-timeline-mock-db-v16' // 版本号一升，旧数据作废、下次打开自动重种（重置演示数据就升这里）
const DEMO_CODE = '123456'

// ---------- 工具 ----------
// 「今天」= 学习日（04:00–次日 04:00，记在起始日）
function today() { return studyDate() }
// 时间戳 → 所属学习日
function dayKeyOf(ts) { return studyDate(ts) }

function uid() {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4)
}

// ---------- 种子数据（相对“今天”生成，保证演示永远新鲜） ----------
function seed() {
  const t = today()
  const d = (n) => addDaysStr(t, n)

  const subjects = [
    { id: 1, name: '政治', sortOrder: 0 },
    { id: 2, name: '英语一', sortOrder: 1 },
    { id: 3, name: '数学三', sortOrder: 2 },
    { id: 4, name: '408', sortOrder: 3 }
  ]

  const entries = []
  let eid = 1
  const makeEntry = (o) => {
    entries.push({
      id: eid++,
      userId: 6,
      sessionId: o.sessionId ?? 100 + eid,
      subjectName: o.subjectName,
      type: o.type, // 1=note 2=错题
      content: o.content,
      tagNames: o.tagNames || [],
      images: o.images || [],
      createdAt: `${o.date}T${o.time || '20:10:00'}`,
      round: o.round ?? 0,
      nextDueAt: o.nextDueAt,
      reviewStatus: o.reviewStatus ?? 0
    })
  }

  // 节奏：第 round 轮到期 = 记录日 + [1,2,4,7,15,22][round] 天；round=6 出队
  // —— 历史记录（自由库 + 进度统计有内容可看）
  makeEntry({ date: d(-12), subjectName: '数学三', type: 2, content: '求极限时用了等价无穷小替换，但替换项是乘除中的因子才能直接换，加减中不行。例题：lim(x→0)(e^x-1-sinx)/x² 答案 1/2。', tagNames: ['极限', '等价无穷小'], round: 6, nextDueAt: null, reviewStatus: 1 })
  makeEntry({ date: d(-12), subjectName: '政治', type: 1, content: '马原：矛盾的普遍性与特殊性辩证关系——共性寓于个性之中，个性也离不开共性。分析题常用“具体问题具体分析”。', tagNames: ['马原', '辩证法'], round: 6, nextDueAt: null, reviewStatus: 1 })
  makeEntry({ date: d(-9), subjectName: '408', type: 2, content: '操作系统：页面置换算法 Belady 异常只出现在 FIFO，LRU/OPT 是栈式算法不会出现。做题时先判断是否栈式。', tagNames: ['OS', '页面置换'], round: 5, nextDueAt: d(-9 + 22), reviewStatus: 0 })
  makeEntry({ date: d(-7), subjectName: '英语一', type: 1, content: '长难句：让步状语从句的倒装（Adj/Adv + as + S + V），例如“Hard as he tried, he failed.”。写作可仿写。', tagNames: ['长难句', '写作'], round: 4, nextDueAt: d(-7 + 15), reviewStatus: 0 })
  makeEntry({ date: d(-5), subjectName: '数学三', type: 2, content: '中值定理证明题：构造辅助函数常用 f(x)e^{g(x)} 或差函数。罗尔定理三条件：闭区间连续、开区间可导、端点值相等。', tagNames: ['中值定理'], round: 3, nextDueAt: d(-5 + 7), reviewStatus: 0 })
  makeEntry({ date: d(-3), subjectName: '政治', type: 2, content: '错题：剩余价值率与利润率混淆。剩余价值率 m′=m/v，利润率 p′=m/(c+v)，利润率总是小于剩余价值率。', tagNames: ['马政经', '剩余价值'], round: 2, nextDueAt: d(-3 + 4), reviewStatus: 0 })

  // —— 今天到期的 3 条（Review 卡有数字可点）
  makeEntry({ date: d(-4), subjectName: '408', type: 2, content: '计算机网络：TCP 三次握手第二步 SYN+ACK 的 ACK 是对 SYN 的确认；第四步不需要是因为 TIME_WAIT 机制兜底。', tagNames: ['计网', 'TCP'], round: 1, nextDueAt: d(-4 + 2), reviewStatus: 0 })
  makeEntry({ date: d(-2), subjectName: '英语一', type: 1, content: '作文素材：坚持类话题可引 “Rome was not built in a day.”；环保类引 “We do not inherit the earth from our ancestors; we borrow it from our children.”', tagNames: ['写作', '素材'], round: 0, nextDueAt: d(-2 + 1), reviewStatus: 0 })
  makeEntry({ date: d(-2), subjectName: '数学三', type: 2, content: '二重积分换序：先画积分区域草图再定限，极坐标适用“圆、扇形、被积函数含 x²+y²”。', tagNames: ['二重积分'], round: 0, nextDueAt: d(-2 + 1), reviewStatus: 0 })

  // —— 近几天记录（进度模块有柱子）
  makeEntry({ date: d(-1), subjectName: '数学三', type: 1, content: '特征值与特征向量：|A-λE|=0 解出 λ 后解齐次方程组；相似对角化的充要条件是 n 个线性无关特征向量。', tagNames: ['线代', '特征值'] })
  makeEntry({ date: d(-1), subjectName: '408', type: 2, content: '数据结构：KMP 的 next 数组是“最长相等前后缀”，匹配失配时主串指针不回溯。', tagNames: ['数据结构', 'KMP'] })

  const sessions = [
    { id: 11, userId: 6, subjectId: 3, subjectName: '数学三', startedAt: `${d(-12)}T08:30:00`, durationSeconds: 7200 },
    { id: 12, userId: 6, subjectId: 1, subjectName: '政治', startedAt: `${d(-12)}T20:00:00`, durationSeconds: 3600 },
    { id: 13, userId: 6, subjectId: 4, subjectName: '408', startedAt: `${d(-9)}T14:00:00`, durationSeconds: 5400 },
    { id: 14, userId: 6, subjectId: 2, subjectName: '英语一', startedAt: `${d(-7)}T19:30:00`, durationSeconds: 4500 },
    { id: 15, userId: 6, subjectId: 3, subjectName: '数学三', startedAt: `${d(-5)}T09:00:00`, durationSeconds: 6300 },
    { id: 16, userId: 6, subjectId: 1, subjectName: '政治', startedAt: `${d(-3)}T21:00:00`, durationSeconds: 2700 },
    { id: 17, userId: 6, subjectId: 4, subjectName: '408', startedAt: `${d(-2)}T15:00:00`, durationSeconds: 4800 },
    { id: 18, userId: 6, subjectId: 2, subjectName: '英语一', startedAt: `${d(-1)}T10:00:00`, durationSeconds: 3900 },
    { id: 19, userId: 6, subjectId: 3, subjectName: '数学三', startedAt: `${d(-1)}T20:30:00`, durationSeconds: 5100 }
  ]

  // —— 更早的 4 个月历史 session：让贡献墙/进度图有数据可看
  // 简单可复现的伪随机（不用 Math.random，保证每次种子一致）
  let s = 20261025
  const rnd = () => (s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff
  let histId = 20
  const histSubjects = [
    { id: 3, name: '数学三' }, { id: 1, name: '政治' },
    { id: 4, name: '408' }, { id: 2, name: '英语一' }
  ]
  for (let back = 13; back <= 150; back++) {
    // 一周学 4~6 天；偶尔摆烂一天
    const r = rnd()
    if (r < 0.28) continue
    const sub = histSubjects[Math.floor(rnd() * histSubjects.length)]
    const mins = 40 + Math.floor(rnd() * 160) // 40~200 分钟
    sessions.push({
      id: histId++,
      userId: 6,
      subjectId: sub.id,
      subjectName: sub.name,
      startedAt: `${d(-back)}T${rnd() > 0.5 ? '09' : '19'}:${String(10 + Math.floor(rnd() * 45)).padStart(2, '0')}:00`,
      durationSeconds: mins * 60
    })
    // 约 1/3 的学习日再来一段
    if (rnd() > 0.66) {
      const sub2 = histSubjects[Math.floor(rnd() * histSubjects.length)]
      sessions.push({
        id: histId++,
        userId: 6,
        subjectId: sub2.id,
        subjectName: sub2.name,
        startedAt: `${d(-back)}T${rnd() > 0.5 ? '15' : '20'}:${String(10 + Math.floor(rnd() * 45)).padStart(2, '0')}:00`,
        durationSeconds: (30 + Math.floor(rnd() * 120)) * 60
      })
    }
  }

  return {
    users: [
      {
        id: 6, email: 'demo@kaoyan.com', password: 'demo123456',
        examYear: 2027, role: 0,
        nickname: 'demo',
        avatarUrl: '/avatar-default.png',
        subjects
      },
      {
        // 演示超管（2026-10-27 三级角色）：登录后设置页出现「进入管理后台」入口
        id: 7, email: 'admin@bigtree.com', password: import.meta.env?.VITE_SUPER_ADMIN_PASSWORD || 'admin123456',
        examYear: 2027, role: 2,
        nickname: '园丁',
        avatarUrl: '/avatar-default.png',
        subjects: subjects.map(s => ({ ...s, userId: 7 }))
      }
    ],
    tokens: {},
    codes: {}, // 'email:purpose' -> { code, expiresAt }
    uploads: {}, // objectKey -> previewUrl（头像等已上传对象的预览）
    checkIns: [
      // 近两周签到（给签到日历/连续签到演示）：昨天连签 6 天，今天留给用户签
      ...[-12, -11, -9, -8, -6, -5, -4, -3, -2, -1].map(n => ({ userId: 6, date: d(n) }))
    ],
    sessions,
    entries,
    nextSessionId: histId + 10,
    nextEntryId: eid,
    nextUserId: 8,
    articles: [
      {
        id: 1, authorName: '园丁',
        title: '2027 考研预报名已开启，这些材料提前备好',
        content: '预报名一般面向应届生，往届生也可参加。请提前准备：身份证、学历学籍信息、户籍与档案所在地、报考点选择。\n\n注意三点：\n1. 报名号生成后牢记，网上确认时要用；\n2. 报考点名额有限，热门城市可能满员，尽量早报；\n3. 生成报名号但未缴费视为报名无效。\n\n正式报名期间可修改部分信息，但报考单位、考试方式等关键项不能改，只能重报。',
        coverObjectKey: null, coverUrl: null,
        status: 1, publishedAt: `${d(-4)}T10:00:00`, updatedAt: `${d(-4)}T10:00:00`
      },
      {
        id: 2, authorName: '小笔',
        title: '冲刺期每日时间表参考（供参考，别硬抄）',
        content: '很多同学问“一天学多久才够”。答案是：比昨天多一点就够。\n\n一个可执行的骨架：\n· 上午 3h：数学/专业课硬骨头（脑子最清醒）\n· 下午 2.5h：英语阅读/翻译或专业课\n· 晚上 2h：政治选择/错题复盘\n· 睡前 0.5h：翻今天记的 note（正好走一遍艾宾浩斯）\n\n关键是“开始”而不是“完美”。状态差的时候先做 25 分钟试试。',
        coverObjectKey: null, coverUrl: null,
        status: 1, publishedAt: `${d(-8)}T20:00:00`, updatedAt: `${d(-8)}T20:00:00`
      },
      {
        id: 3, authorName: '园丁',
        title: '关于心态：焦虑是正常的',
        content: '考研是场持久战。你不是一个人在焦虑。\n\n几个小建议：\n· 和昨天的自己比，不和别人比；\n· 累了就休息，休息不是犯罪；\n· 记录（比如用 Big Tree 记 note/错题）本身就是一种整理焦虑的方式。\n\n祝顺利。',
        coverObjectKey: null, coverUrl: null,
        status: 1, publishedAt: `${d(-15)}T09:00:00`, updatedAt: `${d(-15)}T09:00:00`
      },
      {
        id: 4, authorName: '小笔',
        title: '（草稿）12 月准考证打印提醒',
        content: '这是一篇草稿，普通用户看不到。',
        coverObjectKey: null, coverUrl: null,
        status: 0, publishedAt: null, updatedAt: t
      }
    ],
    nextArticleId: 5
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

// ---------- 业务工具 ----------
const INTERVALS = [1, 2, 4, 7, 15, 22] // 艾宾浩斯六轮（相对记录日）

function buildTags() {
  const d = loadDb()
  const names = [...new Set(d.entries.flatMap(e => e.tagNames))].sort((a, b) => a.localeCompare(b, 'zh'))
  return names.map((name, i) => ({ id: i + 1, name }))
}

function entryListItem(e) {
  return {
    id: e.id,
    subjectName: e.subjectName,
    type: e.type,
    content: e.content,
    tags: e.tagNames,
    imageCount: e.images.length,
    createdAt: e.createdAt,
    round: e.round,
    reviewStatus: e.reviewStatus
  }
}

function entryDetail(e) {
  return {
    id: e.id,
    subjectName: e.subjectName,
    type: e.type,
    content: e.content,
    tags: e.tagNames,
    images: e.images.map(im => ({ objectKey: im.objectKey, previewUrl: im.previewUrl })),
    createdAt: e.createdAt,
    round: e.round,
    reviewStatus: e.reviewStatus
  }
}

// ---------- 路由表 ----------
const routes = {
  // ---- §3 认证 ----
  'POST /api/auth/send-code': (ctx) => {
    const { email, purpose } = ctx.body
    if (!email || !purpose) return fail(1001, '邮箱和用途必填')
    const d = loadDb()
    const exists = d.users.some(u => u.email === email)
    if (purpose === 1 && exists) return fail(3001, '该邮箱已注册')
    if (purpose === 2 && !exists) return fail(3002, '该邮箱未注册')

    const key = `${email}:${purpose}`
    const old = d.codes[key]
    const now = Date.now()
    if (old && now < old.expiresAt - 540000) { // 60 秒内重复发（10 分钟有效 - 9 分钟 = 60s 限频）
      return fail(3003, '发送太频繁，请 60 秒后再试')
    }
    d.codes[key] = { code: DEMO_CODE, expiresAt: now + 10 * 60 * 1000 }
    saveDb()
    // 演示模式：验证码直接放 message 里方便体验
    return ok(null, `验证码已发送（演示环境：${DEMO_CODE}）`)
  },

  'POST /api/auth/register': (ctx) => {
    const { email, password, examYear, code, subjects } = ctx.body
    if (!email || !password || !examYear || !code || !Array.isArray(subjects)) return fail(1001, '缺少必填字段')
    if (password.length < 6 || password.length > 32) return fail(1001, '密码需 6~32 位')
    if (subjects.length < 2 || subjects.length > 5) return fail(1001, '科目需 2~5 门')
    const d = loadDb()
    if (d.users.some(u => u.email === email)) return fail(3001, '该邮箱已注册')
    const key = `${email}:1`
    const rec = d.codes[key]
    if (!rec || rec.code !== code || Date.now() > rec.expiresAt) return fail(1001, '验证码无效或已过期')
    delete d.codes[key]

    const id = d.nextUserId++
    d.users.push({
      id, email, password, examYear: Number(examYear), role: 0,
      // 注册流程不变，只自动赋默认昵称/头像（产品：用户表新增字段）
      nickname: email.split('@')[0],
      avatarUrl: '/avatar-default.png',
      subjects: subjects.map((name, i) => ({ id: id * 100 + i, name, sortOrder: i }))
    })
    const token = `mock-token-${id}-${uid()}`
    d.tokens[token] = id
    saveDb()
    return ok({ token, userId: id, email })
  },

  'POST /api/auth/login': (ctx) => {
    const { email, password } = ctx.body
    const d = loadDb()
    const u = d.users.find(x => x.email === email && x.password === password)
    if (!u) return fail(2001, '邮箱或密码错误')
    const token = `mock-token-${u.id}-${uid()}`
    d.tokens[token] = u.id
    saveDb()
    return ok({ token, userId: u.id, email: u.email, role: u.role ?? 0 })
  },

  'POST /api/auth/reset-password': (ctx) => {
    const { email, code, newPassword } = ctx.body
    if (!email || !code || !newPassword) return fail(1001, '缺少必填字段')
    if (newPassword.length < 6 || newPassword.length > 32) return fail(1001, '密码需 6~32 位')
    const d = loadDb()
    const u = d.users.find(x => x.email === email)
    if (!u) return fail(3002, '该邮箱未注册')
    const rec = d.codes[`${email}:2`]
    if (!rec || rec.code !== code || Date.now() > rec.expiresAt) return fail(1001, '验证码无效或已过期')
    delete d.codes[`${email}:2`]
    u.password = newPassword
    saveDb()
    return ok(null, '密码已重置，请用新密码登录')
  },

  // ---- §4 主界面 ----
  'GET /api/home': (ctx) => {
    const u = ctx.user
    const d = loadDb()
    const t = today() // 学习日
    return ok({
      checkedInToday: d.checkIns.some(c => c.userId === u.id && c.date === t),
      todayLearned: d.sessions.some(s => s.userId === u.id && dayKeyOf(s.startedAt) === t),
      dueReviewCount: d.entries.filter(e =>
        e.userId === u.id && e.reviewStatus === 0 && e.nextDueAt && e.nextDueAt <= t
      ).length
    })
  },

  'POST /api/check-ins': (ctx) => {
    const d = loadDb()
    const t = today()
    if (d.checkIns.some(c => c.userId === ctx.user.id && c.date === t)) return fail(3001, '今天已签到')
    d.checkIns.push({ userId: ctx.user.id, date: t })
    saveDb()
    return ok(null)
  },

  // ---- §5 Learn 流程 ----
  'GET /api/subjects': (ctx) => ok(ctx.user.subjects),

  'POST /api/sessions': (ctx) => {
    const { subjectId } = ctx.body
    const d = loadDb()
    const sub = ctx.user.subjects.find(s => s.id === Number(subjectId))
    if (!sub) return fail(1001, '科目不存在')
    const id = d.nextSessionId++
    // 时间戳用真实墙钟（归天由学习日口径负责）
    const now = new Date()
    const startedAt = `${dateStr(now)}T${now.toTimeString().slice(0, 8)}`
    d.sessions.push({ id, userId: ctx.user.id, subjectId: sub.id, subjectName: sub.name, startedAt, durationSeconds: 0 })
    saveDb()
    return ok({ id, startedAt })
  },

  'POST /api/sessions/:id/finish': (ctx) => {
    const d = loadDb()
    const s = d.sessions.find(x => x.id === Number(ctx.params.id) && x.userId === ctx.user.id)
    if (!s) return fail(3002, '会话不存在')
    s.durationSeconds = Math.max(0, Number(ctx.body.durationSeconds) || 0)
    saveDb()
    return ok(null)
  },

  'POST /api/sessions/:id/entries': (ctx) => {
    const d = loadDb()
    const s = d.sessions.find(x => x.id === Number(ctx.params.id) && x.userId === ctx.user.id)
    if (!s) return fail(3002, '会话不存在')
    const list = ctx.body.entries
    if (!Array.isArray(list)) return fail(1001, 'entries 必须是数组')
    const t = today() // 学习日（记录归属日 + 首轮到期基准）
    const now = new Date()
    const stamp = `${dateStr(now)}T${now.toTimeString().slice(0, 8)}` // 真实墙钟时间戳
    const created = []
    for (const item of list) {
      const content = (item.content || '').trim()
      const images = (item.imageObjectKeys || []).map(k => ({ objectKey: k, previewUrl: k }))
      if (!content && images.length === 0) return fail(1001, '文字与图片至少填一项')
      if (images.length > 10) return fail(1001, '每条记录最多 10 张图片')
      const id = d.nextEntryId++
      d.entries.push({
        id, userId: ctx.user.id, sessionId: s.id,
        subjectName: s.subjectName,
        type: Number(item.type),
        content,
        tagNames: item.tagNames || [],
        images,
        createdAt: stamp,
        round: 0,
        nextDueAt: addDaysStr(t, INTERVALS[0]),
        reviewStatus: 0
      })
      created.push(id)
    }
    saveDb()
    return ok(created)
  },

  'POST /api/uploads/images': (ctx) => {
    // 演示环境：小文件读成 dataURL（刷新后仍在）；大文件退回 blob URL（当次会话有效）
    const file = ctx.body && ctx.body.file
    const objectKey = `user_${ctx.user.id}/tmp/${uid()}.webp`
    const done = (previewUrl) => {
      const d = loadDb()
      d.uploads[objectKey] = previewUrl
      saveDb()
      return ok({
        objectKey,
        contentType: 'image/webp',
        sizeBytes: (file && file.size) || 120000,
        previewUrl
      })
    }
    if (file && typeof FileReader !== 'undefined' && file.size < 300 * 1024) {
      return new Promise((resolve) => {
        const fr = new FileReader()
        fr.onload = () => resolve(done(fr.result))
        fr.onerror = () => resolve(done('/wallpaper.png'))
        fr.readAsDataURL(file)
      })
    }
    const previewUrl = file && typeof URL !== 'undefined' && URL.createObjectURL
      ? URL.createObjectURL(file)
      : '/wallpaper.png'
    return done(previewUrl)
  },

  // ---- 用户资料（2026-10-25 增补：昵称/头像）----
  'GET /api/profile': (ctx) => {
    const u = ctx.user
    return ok({
      userId: u.id,
      email: u.email,
      nickname: u.nickname || u.email.split('@')[0],
      avatarUrl: u.avatarUrl || '/avatar-default.png',
      examYear: u.examYear
    })
  },

  'PUT /api/profile': (ctx) => {
    const u = ctx.user
    const { nickname, avatarObjectKey } = ctx.body
    if (nickname !== undefined) {
      const n = String(nickname).trim()
      if (!n || n.length > 20) return fail(1001, '昵称需 1~20 个字符')
      u.nickname = n
    }
    if (avatarObjectKey) {
      u.avatarObjectKey = avatarObjectKey
      u.avatarUrl = loadDb().uploads[avatarObjectKey] || '/avatar-default.png'
    }
    saveDb()
    return ok({ userId: u.id, nickname: u.nickname || u.email.split('@')[0], avatarUrl: u.avatarUrl || '/avatar-default.png' })
  },

  // ---- §6 Review ----
  'GET /api/reviews/due': (ctx) => {
    const d = loadDb()
    const t = today()
    return ok(d.entries
      .filter(e => e.userId === ctx.user.id && e.reviewStatus === 0 && e.nextDueAt && e.nextDueAt <= t)
      .sort((a, b) => a.nextDueAt.localeCompare(b.nextDueAt))
      .map(entryDetail))
  },

  'POST /api/reviews/:entryId/complete': (ctx) => {
    const d = loadDb()
    const e = d.entries.find(x => x.id === Number(ctx.params.entryId) && x.userId === ctx.user.id)
    if (!e) return fail(3002, '记录不存在')
    e.round += 1
    if (e.round >= INTERVALS.length) {
      e.round = INTERVALS.length
      e.reviewStatus = 1
      e.nextDueAt = null
      saveDb()
      return ok({ round: e.round, nextDueAt: null })
    }
    // 相对记录日顺延（契约 §6.2）；记录日 = 学习日
    const recordDate = dayKeyOf(e.createdAt)
    e.nextDueAt = addDaysStr(recordDate, INTERVALS[e.round])
    saveDb()
    return ok({ round: e.round, nextDueAt: e.nextDueAt })
  },

  // ---- §7 自由库 + 标签 ----
  'GET /api/entries': (ctx) => {
    const d = loadDb()
    const q = ctx.query
    let list = d.entries.filter(e => e.userId === ctx.user.id)
    if (q.subjectId) list = list.filter(e => {
      const sub = ctx.user.subjects.find(s => s.id === Number(q.subjectId))
      return sub && e.subjectName === sub.name
    })
    if (q.type) list = list.filter(e => e.type === Number(q.type))
    if (q.tagIds) {
      const tagMap = new Map(buildTags().map(t => [String(t.id), t.name]))
      const names = String(q.tagIds).split(',').map(id => tagMap.get(id)).filter(Boolean)
      list = list.filter(e => e.tagNames.some(n => names.includes(n)))
    }
    list = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    const page = Math.max(1, Number(q.page) || 1)
    const size = Math.min(50, Math.max(1, Number(q.size) || 10))
    return ok({
      items: list.slice((page - 1) * size, page * size).map(entryListItem),
      total: list.length,
      page,
      size
    })
  },

  'GET /api/entries/:id': (ctx) => {
    const d = loadDb()
    const e = d.entries.find(x => x.id === Number(ctx.params.id) && x.userId === ctx.user.id)
    if (!e) return fail(3002, '记录不存在')
    return ok(entryDetail(e))
  },

  'PUT /api/entries/:id': (ctx) => {
    const d = loadDb()
    const e = d.entries.find(x => x.id === Number(ctx.params.id) && x.userId === ctx.user.id)
    if (!e) return fail(3002, '记录不存在')
    const { content, tagNames, imageObjectKeys } = ctx.body
    const text = (content || '').trim()
    const imgs = imageObjectKeys || []
    if (!text && imgs.length === 0) return fail(1001, '文字与图片至少填一项')
    if (imgs.length > 10) return fail(1001, '每条记录最多 10 张图片')
    e.content = text
    e.tagNames = tagNames || []
    // 整体替换式：已有的 preview 保留，新 key 用自身当预览
    const old = new Map(e.images.map(im => [im.objectKey, im.previewUrl]))
    e.images = imgs.map(k => ({ objectKey: k, previewUrl: old.get(k) || k }))
    // 复习状态不因编辑改变（契约 §7.3）
    saveDb()
    return ok(null)
  },

  'DELETE /api/entries/:id': (ctx) => {
    const d = loadDb()
    const i = d.entries.findIndex(x => x.id === Number(ctx.params.id) && x.userId === ctx.user.id)
    if (i < 0) return fail(3002, '记录不存在')
    d.entries.splice(i, 1)
    saveDb()
    return ok(null)
  },

  'GET /api/tags': (ctx) => {
    let tags = buildTags()
    const q = ctx.query.q
    if (q) tags = tags.filter(t => t.name.includes(String(q)))
    return ok(tags)
  },

  // ---- §8 进度 ----
  'GET /api/progress': (ctx) => {
    const d = loadDb()
    const q = ctx.query
    const to = q.to || today()
    const from = q.from || addDaysStr(today(), -new Date().getDate() + 1) // 默认本月 1 日
    const days = []
    const checkInDates = []
    for (let cur = from; cur <= to; cur = addDaysStr(cur, 1)) {
      // 归桶按学习日：凌晨 04:00 前的记录/学习算前一天
      const daySessions = d.sessions.filter(s => s.userId === ctx.user.id && dayKeyOf(s.startedAt) === cur)
      const dayEntries = d.entries.filter(e => e.userId === ctx.user.id && dayKeyOf(e.createdAt) === cur)
      const bySubjectMap = new Map()
      for (const s of daySessions) {
        bySubjectMap.set(s.subjectName, (bySubjectMap.get(s.subjectName) || 0) + s.durationSeconds)
      }
      if (d.checkIns.some(c => c.userId === ctx.user.id && c.date === cur)) checkInDates.push(cur)
      days.push({
        date: cur,
        totalSeconds: daySessions.reduce((a, s) => a + s.durationSeconds, 0),
        bySubject: [...bySubjectMap.entries()].map(([subjectName, seconds]) => ({ subjectName, seconds })),
        noteCount: dayEntries.filter(e => e.type === 1).length,
        wrongCount: dayEntries.filter(e => e.type === 2).length
      })
    }
    return ok({ days, checkInDates })
  },

  // ---- §9 信息模块 ----
  'GET /api/info/overview': (ctx) => {
    const examYear = ctx.user.examYear
    // 初试：届数前一年的 12 月（2027 届 → 2026-12-19）
    const y = examYear - 1
    const milestones = [
      { name: '预报名', dateFrom: `${y}-09-24`, dateTo: `${y}-09-27`, sortOrder: 1 },
      { name: '正式报名', dateFrom: `${y}-10-15`, dateTo: `${y}-10-28`, sortOrder: 2 },
      { name: '网上确认', dateFrom: `${y}-11-02`, dateTo: `${y}-11-08`, sortOrder: 3 },
      { name: '准考证下载', dateFrom: `${y}-12-14`, dateTo: `${y}-12-20`, sortOrder: 4 },
      { name: '初试', dateFrom: `${y}-12-19`, dateTo: `${y}-12-20`, sortOrder: 5 },
      { name: '出分', dateFrom: `${examYear}-02-20`, dateTo: `${examYear}-02-28`, sortOrder: 6 },
      { name: '国家线', dateFrom: `${examYear}-03-15`, dateTo: null, sortOrder: 7 },
      { name: '调剂系统开放', dateFrom: `${examYear}-04-06`, dateTo: null, sortOrder: 8 }
    ]
    const first = milestones.find(m => m.name === '初试')
    const [Y, M, D] = first.dateFrom.split('-').map(Number)
    // 倒计时基准 = 学习日「今天」
    const daysLeft = Math.round((new Date(Y, M - 1, D) - parseDate(today())) / 86400000)
    return ok({ examYear, daysLeft, milestones })
  },

  'GET /api/articles': (ctx) => {
    const d = loadDb()
    const q = ctx.query
    const list = d.articles
      .filter(a => a.status === 1)
      .sort((a, b) => (b.publishedAt || '').localeCompare(a.publishedAt || ''))
    const page = Math.max(1, Number(q.page) || 1)
    const size = Math.min(50, Number(q.size) || 10)
    return ok({
      items: list.slice((page - 1) * size, page * size).map(a => ({
        id: a.id, title: a.title, authorName: a.authorName || '未知', coverUrl: a.coverUrl, publishedAt: a.publishedAt
      })),
      total: list.length, page, size
    })
  },

  'GET /api/articles/:id': (ctx) => {
    const d = loadDb()
    const a = d.articles.find(x => x.id === Number(ctx.params.id))
    if (!a || a.status !== 1) return fail(3002, '文章不存在或未发布')
    return ok({ id: a.id, title: a.title, authorName: a.authorName || '未知', content: a.content, coverUrl: a.coverUrl, publishedAt: a.publishedAt })
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
/**
 * 跨端票据登记（2026-10-27 方案 A 反向：管理后台「回到用户端」）：
 * 把票据身份登记进 mock 会话表，随后带该 token 的请求即可通行（与 web-admin mock 对称）。
 */
export function mockAdoptSession({ token, userId, email, role }) {
  if (!token || !userId) return
  const d = loadDb()
  d.tokens[token] = userId
  if (!d.users.find(u => u.id === userId)) {
    d.users.push({
      id: userId,
      email: email || `user${userId}@bigtree.com`,
      password: '',
      examYear: 2027,
      role: role ?? 0,
      nickname: (email && email.split('@')[0]) || `用户${userId}`,
      avatarUrl: '/avatar-default.png',
      subjects: []
    })
  }
  saveDb()
}

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

/** 重置演示数据（设置/调试用） */
export function resetMockDb() {
  localStorage.removeItem(STORE_KEY)
  db = null
}
