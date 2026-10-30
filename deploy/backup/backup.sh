#!/usr/bin/env bash
# Big Tree 每日备份（cron 驱动）：MySQL dump + MinIO 对象打包，本地滚动保留 7 天
# 用法：直接执行 ./backup.sh（cron 示例：17 3 * * * /home/ubuntu/bigtree/backup/backup.sh >> /home/ubuntu/bigtree/backup/backup.log 2>&1）
set -euo pipefail

BACKUP_DIR="$(cd "$(dirname "$0")" && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"
KEEP_DAYS=7

# cron 环境无变量：从部署目录 .env 加载（MYSQL_ROOT_PASSWORD 等）
set -a
# shellcheck disable=SC1091
source "$BACKUP_DIR/../.env"
set +a

echo "[$(date '+%F %T')] 备份开始"

# 1) MySQL dump（经容器执行，gzip 压缩）
docker exec bt-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers --default-character-set=utf8mb4 \
  exam_timeline | gzip > "$BACKUP_DIR/mysql-$STAMP.sql.gz"
echo "MySQL dump 完成: mysql-$STAMP.sql.gz ($(du -h "$BACKUP_DIR/mysql-$STAMP.sql.gz" | cut -f1))"

# 2) MinIO 数据打包（minio 镜像是 scratch 无 tar，借 alpine 挂卷打包）
docker run --rm -v bigtree_minio-data:/data -v "$BACKUP_DIR":/backup alpine \
  tar czf "/backup/minio-$STAMP.tar.gz" -C /data .
echo "MinIO 打包完成: minio-$STAMP.tar.gz ($(du -h "$BACKUP_DIR/minio-$STAMP.tar.gz" | cut -f1))"

# 3) 滚动清理 N 天前的旧备份
find "$BACKUP_DIR" -name "mysql-*.gz" -mtime +"$KEEP_DAYS" -delete
find "$BACKUP_DIR" -name "minio-*.gz" -mtime +"$KEEP_DAYS" -delete
echo "[$(date '+%F %T')] 备份结束（保留最近 $KEEP_DAYS 天）"
