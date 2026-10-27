# Big Tree

> Big Tree，Be a Tree —— 考研时间和知识点规划助手。记录学过的，复习该忘的。

面向考研的多用户学习时间线系统：Learn 计时记录 note/错题 → 艾宾浩斯六轮（1/2/4/7/15/22 天）自动安排复习 → 统计页沉淀学习进度。设计哲学借鉴《不背单词》：系统替用户安排复习，用户只负责学和记。

## 仓库结构

| 目录 | 内容 |
|---|---|
| `docs/` | 产品设计文档（**唯一权威**）、接口文档、数据库设计、开发进度、联调手册 |
| `server/` | Spring Boot 后端（REST API + JWT 邮箱登录 + MinIO 图片存储），DDL 见 `src/main/resources/schema.sql` |
| `web-user/` | Vue3 用户端（手机 / PC 响应式一套代码，Vite + Pinia，UI 手写不背单词风格） |
| `brand/` | 品牌 Big Tree：logo SVG + 品牌规范（设计铁律见 `preview.html`） |
| `pics/` | 壁纸素材（自定义背景图池） |
| `temp/` | 验证脚本（`verify_*.sh` + 输出）与一次性产物 |

## 本地开发（Windows）

1. 起容器：`docker start exam-mysql exam-minio`（MySQL 宿主 3307）
2. 起后端（Git Bash，须先加载 `.env` 占位，另注意 `--server.port=8080` 必须显式给）：
   ```bash
   cd server
   set -a && source ../.env && set +a
   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
   ```
3. 起前端（任意终端）：`cd web-user && npm run dev`（5173，`/api` 代理 8080；mock/real 模式由 `web-user/.env` 的 `VITE_API_MODE` 决定）

## 文档入口

- 产品设计（权威，先读它）：`docs/考研时间线系统-产品设计文档.md`
- 接口契约与后端完成度：`docs/接口文档.md`（看总览表状态列）
- 数据库设计（10 表 + 统计口径）：`docs/数据库设计.md`
- 开发进度（每日流水 + 待办）：`docs/开发进度.md`
