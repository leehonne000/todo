#!/usr/bin/env bash
# ============================================================
# 纯 MinIO 镜像 · 可复现构建配方（2026-10-26）
#
# 背景：MinIO 官方 2025-10 起停发免费容器镜像（Docker Hub/quay/ECR/ghcr/
#       dl.min.io 全部不可用），第三方重建 Minimus 也于 2026-10-22 关停。
#       本配方从官方源码（经 goproxy.cn 模块代理）编译出纯净镜像：
#       成品 = scratch 空镜像 + minio 二进制 + CA 证书，无 shell/无其他软件。
#
# 用法：bash build.sh          （重新编译 + 打镜像）
# 产物：minio/minio:latest 与 minio/minio:<版本> 标签（本地，可 docker save 分发）
# ============================================================
set -euo pipefail

BASE="$(cd "$(dirname "$0")" && pwd)"
# MinIO 官方源码伪版本（goproxy.cn @latest 解析，对应官方 master 7aac2a2c5b7c）
MINIO_VER="v0.0.0-20260212201848-7aac2a2c5b7c"
GO_VER="go1.27.3"
SRC="$BASE/src/github.com/minio/minio@${MINIO_VER}"

# ---- 1. Go 工译器（官方中国镜像下载，解压到 temp，不装进系统）----
if [ ! -x "$BASE/toolchain/go/bin/go.exe" ]; then
  echo ">> 下载 ${GO_VER} ..."
  curl -sSL -o "$BASE/${GO_VER}.windows-amd64.zip" \
    "https://golang.google.cn/dl/${GO_VER}.windows-amd64.zip"
  python -c "import zipfile; zipfile.ZipFile(r'$BASE/${GO_VER}.windows-amd64.zip').extractall(r'$BASE/toolchain')"
fi

# ---- 2. MinIO 源码（goproxy.cn 模块代理，绕开被墙的 GitHub）----
if [ ! -d "$SRC" ]; then
  echo ">> 下载 MinIO 源码 ${MINIO_VER} ..."
  curl -sSL -o "$BASE/minio-src.zip" \
    "https://goproxy.cn/github.com/minio/minio/@v/${MINIO_VER}.zip"
  python -c "import zipfile; zipfile.ZipFile(r'$BASE/minio-src.zip').extractall(r'$BASE/src')"
fi

# ---- 3. CA 证书包（scratch 镜像里给 minio 做 TLS 出站用）----
[ -f "$BASE/ca-certificates.crt" ] || cp /mingw64/etc/ssl/certs/ca-bundle.crt "$BASE/ca-certificates.crt"

# ---- 4. 交叉编译 linux/amd64 静态二进制（官方 Makefile 同款参数）----
# CGO_ENABLED=0 → 不依赖任何系统库；-tags kqueue / -trimpath 同官方构建
export GOROOT="$BASE/toolchain/go"
export GOCACHE="$BASE/gocache" GOMODCACHE="$BASE/gomodcache"
export GOPROXY="https://goproxy.cn,direct" GOSUMDB=off GOTOOLCHAIN=local
export CGO_ENABLED=0 GOOS=linux GOARCH=amd64 GOFLAGS=-buildvcs=false
(
  cd "$SRC"
  "$GOROOT/bin/go.exe" build -tags kqueue -trimpath \
    -ldflags "-s -w \
      -X github.com/minio/minio/cmd.Version=${MINIO_VER} \
      -X github.com/minio/minio/cmd.ReleaseTag=DEVELOPMENT.2026-02-12T20-18-48Z \
      -X github.com/minio/minio/cmd.CommitID=7aac2a2c5b7c \
      -X github.com/minio/minio/cmd.ShortCommitID=7aac2a2c5b7c \
      -X github.com/minio/minio/cmd.CopyrightYear=2026" \
    -o "$BASE/minio" .
)

# ---- 5. 打镜像：FROM scratch，成品盒里只有二进制 + 证书 ----
docker build -t "minio/minio:latest" -t "minio/minio:${MINIO_VER}" "$BASE"

echo ""
echo "== 完成 =="
docker images minio/minio
echo "试跑："
echo "  docker run -d --name exam-minio --env-file .env -p 9000:9000 -p 9001:9001 -v exam-minio-data:/data minio/minio:latest server /data --console-address \":9001\""
