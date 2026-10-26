# 联调手册 —— 登录全流程 & Learn 全流程

> 用法：按顺序一步步操作，开着浏览器控制台对照；发现 **404 / 500 / code≠0** 就按 §6 模板记下来反馈。
> 服务地址：前端 http://localhost:5173/ ｜ 数据库 `127.0.0.1:3307` 库 `exam_timeline`（root 密码见 `.env`，或用 §2 的 docker 命令）

---

## 0. 开测前 3 分钟准备

1. **无痕/隐私窗口**打开 http://localhost:5173/ （防旧缓存，重要）
2. F12 打开 DevTools：**Console**（看报错）+ **Network**（勾选 `Preserve log`，筛 Fetch/XHR）
3. 开一个能查库的窗口（MySQL 客户端连 3307，或每步用命令）：
   ```bash
   docker exec -it exam-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 exam_timeline'
   ```

## 1. 状态码速查（看到什么算错）

| 现象 | 含义 | 要不要反馈 |
|---|---|---|
| HTTP 200 + `"code":0` | 成功 | — |
| HTTP 200 + code≠0 | 业务错误（message 是人话原因） | 与预期不符才反馈 |
| code 1001 | 参数/校验错 | 看提示是否合理 |
| code 2001 | 未登录 / 邮箱密码错 | — |
| code 3001 | 冲突（如"今天已签到"） | — |
| code 3002 | 不存在（如"会话不存在"） | ✅ 反馈 |
| **HTTP 404** | 路径不存在（接口拼错/没实现） | ✅ **必反馈，带 URL** |
| HTTP 401 | 没带 token / token 失效 | 未登录操作属正常 |
| HTTP 400 | 参数校验失败 | ✅ 反馈请求体 |
| HTTP 500 / code 5000 | 服务器内部错误 | ✅ **必反馈** |

## 2. 数据表速查

| 表 | 存什么 | 一条验证 SQL |
|---|---|---|
| `t_user` | 账号（密码是 BCrypt 长串；role 0=普通 1=管理员） | `SELECT id,email,nickname,role,LEFT(password,12) pw FROM t_user;` |
| `t_subject` | 科目组合（注册时定，sort_order 排序） | `SELECT * FROM t_subject ORDER BY sort_order;` |
| `t_verify_code` | 邮箱验证码（purpose 1=注册 2=找回） | `SELECT email,code,purpose,expires_at FROM t_verify_code;` |
| `t_study_session` | 一次 Learn 计时（挂科目、净秒数） | `SELECT id,subject_id,started_at,ended_at,duration_seconds FROM t_study_session;` |
| `t_entry` | note/错题记录 ★（含艾宾浩斯状态） | `SELECT id,session_id,subject_id,type,LEFT(content,20) c,round,next_due_at,review_status FROM t_entry;` |
| `t_tag` / `t_entry_tag` | 标签词表 / 记录-标签关联 | `SELECT * FROM t_tag; SELECT * FROM t_entry_tag;` |
| `t_entry_image` | 图片元数据（本体在 MinIO） | `SELECT entry_id,object_key,content_type,size_bytes,sort_order FROM t_entry_image;` |
| `t_check_in` | 每日签到 | `SELECT * FROM t_check_in;` |

图片本体看 MinIO 控制台：http://localhost:9001 （账号密码= `.env` 的 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD）。

---

## 3. Part A：登录全流程

| # | 操作 | Network 预期 | 数据表核对 |
|---|---|---|---|
| A1 | 打开 5173，进登录页 | 无请求 | — **检查：邮箱/密码输入框必须是空的**（本次修复点；聚焦后能正常输入） |
| A2 | 点「注册新账号」进注册页 | 无请求 | 输入框同样为空 |
| A3 | 填**真实 QQ 邮箱** + 密码(6~32) + 届数(2027) + 选 4 门科目 → 点「发验证码」 | `POST /api/auth/send-code` → code=0 | `t_verify_code` +1 行（purpose=1）；**QQ 邮箱收到验证码邮件**（真实发信首次验证！收不到就反馈） |
| A4 | 填 6 位验证码 → 点注册 | `POST /api/auth/register` → code=0，响应含 token | `t_user` +1 行（role=0、昵称=邮箱@前缀）；`t_subject` **+4 行**（你的科目） |
| A5 | 自动进主界面 | `GET /api/home` → `{checkedInToday:false, todayLearned:false, dueReviewCount:0}` | — |
| A6 | 点「签到」 | `POST /api/check-ins` → code=0 | `t_check_in` +1 行（check_in_date=今天学习日） |
| A7 | 再点一次签到 | code=3001「今天已签到」 | 无新行 |
| A8 | 退出登录 → 回登录页 | — | **输入框又应为空** |
| A9 | 故意输错密码登录 | code=2001 | 无变化 |
| A10 | 正确密码登录 | `POST /api/auth/login` → code=0 + token | `GET /api/home` checkedInToday 仍 true |
| A11 | 「忘记密码」→ 发验证码 | `POST /api/auth/send-code`（purpose=2）→ code=0 | `t_verify_code` +1 行（purpose=2） |
| A12 | 填码 + 新密码提交 | `POST /api/auth/reset-password` → code=0 | `t_user` 该行 **password 值变了**（BCrypt 每次不同） |
| A13 | 旧密码登录 / 新密码登录 | 2001 / code=0 | — |
| A14 | 登录后**刷新页面** | `GET /api/home` 200 | 仍在主界面（token 在浏览器里） |
| A15 | （可选）DevTools → Application → Local Storage 把 token 改乱 → 刷新 | 任意接口 2001 | 自动踢回登录页 |

## 4. Part B：Learn 全流程

| # | 操作 | Network 预期 | 数据表核对 |
|---|---|---|---|
| B1 | 主界面点 Learn 卡 → 选科目页 | `GET /api/subjects` → 注册时那 4 门 | 只读 `t_subject`，无新数据 |
| B2 | 点一门科目（如「数学」）→ 进横屏计时 | `POST /api/sessions` → `{id, startedAt}` | `t_study_session` +1 行：subject_id=所选、duration=0、ended_at=NULL |
| B3 | 跑 30 秒 → 暂停 5 秒 → 恢复再 10 秒 → **长按结束** | `POST /api/sessions/{id}/finish` `{durationSeconds}` | 该行 **duration_seconds≈40**（不含暂停）、ended_at 有值。秒数和体感差很多就反馈 |
| B4 | 总结页 →「添加note」→ 写文字 `测试考点A` + 标签 `马原`、`重点`（回车生成）+ **传 2 张图** → 保存 | 每张图 `POST /api/uploads/images` → code=0（sizeBytes>0，缩略图应显示）；保存只是前端暂存 | **表里还没有记录**；图片在 MinIO 的 `user_{id}/tmp/`（控制台可看） |
| B5 | 「添加错题」→ 只传 1 张图、不写文字 → 保存（纯图记录） | 同上上传 | 同上 |
| B6 | 点 **Done** | `POST /api/sessions/{id}/entries` → `data=[id1,id2]` | `t_entry` **+2 行**：type=1/2、subject_id=继承、**round=0、next_due_at=记录日+1、review_status=0**、第一行 content=测试考点A；`t_tag` +2；`t_entry_tag` +2；`t_entry_image` **+3 行**（object_key=`user_{uid}/entry_{eid}/…`）；MinIO 里 **tmp 对象消失**、`entry_{eid}/` 出现 3 个对象 |
| B7 | 自动回主界面 | `GET /api/home` → **todayLearned=true** | 今日学习卡应打勾 ✓ |
| B8 | （可选）再点 Learn 学一轮，验证多 session | — | `t_study_session` 2 行、`t_entry` 累积 |

**艾宾浩斯字段怎么读**（B6 后可看）：`round=0` 还没复习过；`next_due_at=明天`（记录日+1 天，记录日=学习日，凌晨 4 点前算前一天）；`review_status=0` 在队列里。明天到期后 Review 卡才会亮数字——今天看不到是正常的。

## 5. 环境一览

| 服务 | 地址 | 备注 |
|---|---|---|
| 前端 | http://localhost:5173/ | 真模式（mock 已关） |
| 后端 | http://localhost:8080 | 前端自动代理 /api |
| MySQL | 127.0.0.1:3307 | 库 exam_timeline |
| MinIO | localhost:9000（控制台 9001） | 桶 exam-timeline |

## 6. 反馈模板（复制填空）

```
步骤编号：A3 / B4
操作：点了什么、填了什么
Network：POST /api/xxxx  →  HTTP 状态码  →  响应 code/message：
预期：应该发生什么
实际：发生了什么（截图或控制台红字）
```
