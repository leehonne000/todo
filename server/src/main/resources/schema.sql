-- ============================================================
-- 考研时间线系统 · 建表脚本
-- 对应文档：docs/数据库设计.md（字段业务含义以该文档为准）
-- 目标库：MySQL 8.x / InnoDB / utf8mb4
-- 时间口径：时刻用 DATETIME（Asia/Shanghai），按天业务用 DATE
-- ============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS exam_timeline
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
USE exam_timeline;

-- ------------------------------------------------------------
-- 1. t_user 用户
-- 业务绑定：注册制多用户（§1.2）；登录=邮箱+密码；届数驱动倒计时（§5.1）
-- ------------------------------------------------------------
CREATE TABLE t_user (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email         VARCHAR(64)     NOT NULL COMMENT '登录标识：注册/登录/找回密码都用它',
  password_hash VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希，不存明文',
  email_verified TINYINT        NOT NULL DEFAULT 0 COMMENT '邮箱激活标志：0=未激活(禁登录) 1=已激活',
  phone         VARCHAR(20)              DEFAULT NULL COMMENT '手机号（预留，v1 不做短信验证）',
  exam_year     SMALLINT        NOT NULL COMMENT '考研届数（如 2027）：倒计时/时间线取数据的依据',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料变更时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';

-- ------------------------------------------------------------
-- 2. t_subject 科目组合
-- 业务绑定：注册时确定（§1.2）；科目编辑四选一（§4.3）；自由库/统计维度（§5.2/§5.3）
-- ------------------------------------------------------------
CREATE TABLE t_subject (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '归属用户；每用户 2~5 门，注册时录入',
  name       VARCHAR(64)     NOT NULL COMMENT '科目名，用户自定义（政治/英语一/数学三/408…）',
  sort_order TINYINT         NOT NULL DEFAULT 0 COMMENT '展示顺序：四选一列表与统计表头顺序',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_subject_user_name (user_id, name),
  KEY idx_subject_user (user_id),
  CONSTRAINT fk_subject_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目组合';

-- ------------------------------------------------------------
-- 3. t_study_session 学习 session
-- 业务绑定：Learn 横屏计时（§4.1）；进度模块时长统计（§5.3）；今日学习卡打勾来源（§3）
-- 生命周期：开始计时建行 -> 长按结束补 ended_at/duration -> Done 写 entry
-- ------------------------------------------------------------
CREATE TABLE t_study_session (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '归属用户',
  started_at      DATETIME        NOT NULL COMMENT '计时开始时刻（横屏进入计时那一刻）',
  ended_at        DATETIME                 DEFAULT NULL COMMENT '长按结束时刻；异常中断可空',
  duration_seconds INT            NOT NULL DEFAULT 0 COMMENT '净学习秒数（排除暂停），一切时长统计的原始值',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_user_started (user_id, started_at),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学习 session';

-- ------------------------------------------------------------
-- 4. t_entry 记录项（★ 核心表）
-- 业务绑定：学习总结子条目（§4.2）+ 科目编辑内容（§4.3）；
--          自由库本体（§5.2）、艾宾浩斯队列（§6）、进度计数（§5.3）
-- 复习状态机：round 0~6；next_due_at 到期 -> Review 卡消费；
--            复习完成 round+1 顺延；round=6 -> review_status=1, next_due_at=NULL
-- ------------------------------------------------------------
CREATE TABLE t_entry (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id    BIGINT UNSIGNED NOT NULL COMMENT '来自哪次 Learn：把"学了多久"和"学了什么"钉在一起（§8）',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '归属用户（冗余，查询免 join）',
  subject_id    BIGINT UNSIGNED NOT NULL COMMENT '科目编辑四选一的落库值（§4.3）',
  type          TINYINT         NOT NULL COMMENT '类型：1=note 2=错题（科目编辑二选一，§4.3）',
  content       TEXT COMMENT '文字内容，可空（纯图片记录）；应用层校验"文字与图片至少一"',
  round         TINYINT         NOT NULL DEFAULT 0 COMMENT '艾宾浩斯已完成轮数 0~6',
  next_due_at   DATE                     DEFAULT NULL COMMENT '下一轮到期日：入库=记录日+1天，此后按 2/4/7/15/22 顺延（§6.1）',
  review_status TINYINT         NOT NULL DEFAULT 0 COMMENT '复习状态：0=队列中 1=已出队（六轮走完）',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间（Done 时刻）= 复习排期基准日',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '自由库可改内容，记最后编辑时间',
  PRIMARY KEY (id),
  KEY idx_entry_review (user_id, review_status, next_due_at),
  KEY idx_entry_subject_type (user_id, subject_id, type),
  KEY idx_entry_session (user_id, session_id),
  CONSTRAINT fk_entry_session FOREIGN KEY (session_id) REFERENCES t_study_session (id) ON DELETE CASCADE,
  CONSTRAINT fk_entry_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_entry_subject FOREIGN KEY (subject_id) REFERENCES t_subject (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记录项（note/错题），核心表';

-- ------------------------------------------------------------
-- 5. t_tag 标签（用户私有词表）
-- 业务绑定：科目编辑自定义标签（§4.3）；标签查询（产品文档 §3.1）；自由库筛选（§5.2）
-- 独立词表：同名复用（UNIQUE），记录删除不影响标签
-- ------------------------------------------------------------
CREATE TABLE t_tag (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '归属用户：标签是用户私有词表',
  name       VARCHAR(32)     NOT NULL COMMENT '标签文本（chip 上的字）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_user_name (user_id, name),
  KEY idx_tag_user (user_id),
  CONSTRAINT fk_tag_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标签词表';

-- ------------------------------------------------------------
-- 6. t_entry_tag 记录-标签关联（多对多中间表）
-- ------------------------------------------------------------
CREATE TABLE t_entry_tag (
  entry_id   BIGINT UNSIGNED NOT NULL,
  tag_id     BIGINT UNSIGNED NOT NULL,
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (entry_id, tag_id),
  KEY idx_entry_tag_tag (tag_id),
  CONSTRAINT fk_entry_tag_entry FOREIGN KEY (entry_id) REFERENCES t_entry (id) ON DELETE CASCADE,
  CONSTRAINT fk_entry_tag_tag FOREIGN KEY (tag_id) REFERENCES t_tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记录-标签关联（多对多）';

-- ------------------------------------------------------------
-- 7. t_entry_image 图片元数据（本体在 MinIO 私有桶 exam-timeline）
-- 业务绑定：科目编辑图片插入（§4.3，上限 10 张/条）
-- ------------------------------------------------------------
CREATE TABLE t_entry_image (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entry_id      BIGINT UNSIGNED NOT NULL COMMENT '所属记录项；最多 10 张（应用层校验）',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '归属用户（鉴权免回表）',
  object_key    VARCHAR(255)    NOT NULL COMMENT 'MinIO 对象键：user_{uid}/entry_{eid}/{uuid}.webp',
  original_name VARCHAR(255)             DEFAULT NULL COMMENT '上传时原始文件名（展示/下载）',
  content_type  VARCHAR(40)     NOT NULL COMMENT 'MIME 类型（如 image/webp），预签名 GET 回填 Content-Type',
  size_bytes    INT             NOT NULL COMMENT '压缩后对象大小（容量统计/排错）',
  sort_order    TINYINT         NOT NULL DEFAULT 0 COMMENT '缩略图顺序 0~9',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_image_object_key (object_key),
  KEY idx_image_entry (entry_id, sort_order),
  CONSTRAINT fk_image_entry FOREIGN KEY (entry_id) REFERENCES t_entry (id) ON DELETE CASCADE,
  CONSTRAINT fk_image_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图片元数据（MinIO 指路牌）';

-- ------------------------------------------------------------
-- 8. t_check_in 签到
-- 业务绑定：主界面每日签到（§3）
-- ------------------------------------------------------------
CREATE TABLE t_check_in (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '归属用户',
  check_in_date DATE            NOT NULL COMMENT '打卡自然日；UNIQUE 天然防重复打卡',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时刻',
  PRIMARY KEY (id),
  UNIQUE KEY uk_checkin_user_date (user_id, check_in_date),
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日签到';

-- ------------------------------------------------------------
-- 9. t_exam_milestone 考研硬节点（★ 全局表：无 user_id，人人共享）
-- 业务绑定：信息模块倒计时与时间线（§5.1）；每年教育部公布后维护数据
-- ------------------------------------------------------------
CREATE TABLE t_exam_milestone (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  exam_year  SMALLINT        NOT NULL COMMENT '届数：与 t_user.exam_year 对齐',
  name       VARCHAR(32)     NOT NULL COMMENT '节点名：预报名/正式报名/网上确认/准考证下载/初试/出分/国家线/复试调剂',
  date_from  DATE            NOT NULL COMMENT '节点起始日；倒计时取 name=初试 的 date_from',
  date_to    DATE                     DEFAULT NULL COMMENT '节点结束日（复试/调剂 3-4 月跨月用；单日节点留空）',
  sort_order TINYINT         NOT NULL DEFAULT 0 COMMENT '时间线展示顺序',
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据维护时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_milestone_year_name (exam_year, name),
  KEY idx_milestone_year (exam_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考研硬节点（全局表，每年维护）';

-- ------------------------------------------------------------
-- 10. t_verify_code 邮箱验证码
-- 业务绑定：注册邮箱激活 + 找回密码（§1.2）；v1 落 MySQL 不引 Redis
-- ------------------------------------------------------------
CREATE TABLE t_verify_code (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email      VARCHAR(64)     NOT NULL COMMENT '收码邮箱（无外键：找回密码时用户未登录）',
  code       VARCHAR(8)      NOT NULL COMMENT '6 位数字验证码',
  purpose    TINYINT         NOT NULL COMMENT '用途：1=注册激活 2=找回密码（防串用）',
  expires_at DATETIME        NOT NULL COMMENT '过期时刻（发码+10 分钟）',
  used_at    DATETIME                 DEFAULT NULL COMMENT '消费时刻；NULL=未用，防重复使用',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发码时间（同邮箱 60s 限频依据）',
  PRIMARY KEY (id),
  KEY idx_code_email_purpose (email, purpose, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='邮箱验证码';

-- ------------------------------------------------------------
-- 参考数据：考研硬节点（每年按教育部公布结果插入，节点名固定 8 个）
-- 示例（日期为示意，使用前替换为官方公布日期）：
-- INSERT INTO t_exam_milestone (exam_year, name, date_from, date_to, sort_order) VALUES
--   (2027, '预报名',     '2026-09-24', '2026-09-27', 1),
--   (2027, '正式报名',   '2026-10-15', '2026-10-24', 2),
--   (2027, '网上确认',   '2026-11-01', '2026-11-05', 3),
--   (2027, '准考证下载', '2026-12-10', NULL,         4),
--   (2027, '初试',       '2026-12-19', '2026-12-20', 5),
--   (2027, '出分',       '2027-02-20', NULL,         6),
--   (2027, '国家线',     '2027-03-15', NULL,         7),
--   (2027, '复试调剂',   '2027-03-20', '2027-04-30', 8);
-- ------------------------------------------------------------
