-- 修复 t_study_session 新增列/表注释（上一条 ALTER 连接字符集为 latin1，注释被存成乱码）
ALTER TABLE t_study_session
  MODIFY COLUMN subject_id BIGINT UNSIGNED NOT NULL COMMENT '本次学习科目（Learn 入口四选一，一次 session 一门）';
ALTER TABLE t_study_session COMMENT='学习 session（单科目）';
