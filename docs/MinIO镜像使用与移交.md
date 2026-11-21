# 纯 MinIO 镜像 · 使用与移交说明

> 读者：任何需要对象存储（S3 兼容）的新项目 / 新 agent。
> 本文自包含，不依赖原项目上下文。目标：10 分钟内把 MinIO 跑起来并接进你的应用。
> 最后核实：2026-11-12。

---

## 1. 这是什么

一个**自建的纯 MinIO 镜像**（对象存储服务，S3 兼容 API + Web 控制台）。

背景：MinIO 官方 2025-10 起停发免费容器镜像（Docker Hub/quay/ECR/ghcr/dl.min.io 全部不可用，第三方 Minimus 也已关停）。本镜像从**官方源码**编译而成，构建配方公开可复现，作为官方渠道断供后的长期替代。

- 成品 = `scratch` 空镜像 + `minio` 静态二进制 + CA 证书，**盒里只有这两个文件**（无 shell、无包管理器、无其他软件）
- 体积约 **150MB**
- 许可：**AGPL-3.0**（详见 §8）

## 2. 镜像身份

| 项 | 值 |
|---|---|
| 本地标签 | `minio/minio:latest`（及 `minio/minio:v0.0.0-20260212201848-7aac2a2c5b7c`） |
| 远端标签 | `ghcr.io/leehonne000/minio:latest`（digest 前缀 `sha256:5b9eb955…`，2026-10-26 推送） |
| 源码版本 | MinIO master @ commit `7aac2a2c5b7c`（Go 模块伪版本 `v0.0.0-20260212201848-7aac2a2c5b7c`） |
| 版本戳 | `minio --version` 输出 `DEVELOPMENT.2026-02-12T20-18-48Z`，CommitID `7aac2a2c5b7c` |
| 架构 | **linux/amd64 单架构**（ARM 见 §9） |
| 内容 | `/usr/bin/minio` + `/etc/ssl/certs/ca-certificates.crt`，无 shell |
| ENTRYPOINT | `["/usr/bin/minio"]`，CMD `["server", "/data"]` |
| 端口 | 9000 = S3 API；9001 = Web 控制台 |
| 构建参数 | Go 1.27.3 交叉编译，`CGO_ENABLED=0`（静态、不依赖系统库），`-tags kqueue -trimpath`，与官方 Makefile 同款 |
| 构建日期 | 2026-10-26 |

## 3. 怎么获取（四条路，按推荐顺序）

### 路线 A：ghcr 拉取（跨机器首选）

```bash
# ghcr 包当前是私有的（匿名拉取 401）——先解决访问权：
# 方式 1（推荐）：在 GitHub → Settings → Developer settings → Packages
#   → leehonne000/minio → Package settings → Change visibility → Public
#   （公开后免登录可拉；AGPL 合规见 §8，构建脚本已随源码出处公开）
# 方式 2：用带 read:packages 权限的 PAT 登录
docker login ghcr.io -u leehonne000 -p <PAT:read:packages>

docker pull ghcr.io/leehonne000/minio:latest
docker tag ghcr.io/leehonne000/minio:latest minio/minio:latest   # 可选：换回短名
```

网络注意：ghcr.io 在中国大陆网络通常需要代理；Docker 守护进程本身不走 shell 代理，需要给 daemon 配 registry-mirror 或代理。

### 路线 B：从原机器 docker save 导出

```bash
# 源机器（Docker Desktop 已启动）
docker save minio/minio:latest -o minio-image.tar      # 约 150MB
# 目标机器
docker load -i minio-image.tar
```

### 路线 C：照配方重新构建（完全可复现，不依赖任何仓库）

配方两个文件（合计几 KB，可整段复制走）：
- `build.sh`——下载 Go 1.27.3（golang.google.cn）→ 拉 MinIO 源码（goproxy.cn，绕开 GitHub）→ 交叉编译 → `docker build`
- `Dockerfile`——`FROM scratch` 打包

在原项目仓库中位于 `temp/minio-image/`。有 Docker + 能访问 golang.google.cn/goproxy.cn 即可重建出一模一样的镜像（源码版本固定为 `7aac2a2c5b7c`）。Linux/macOS 上把 `go.exe` 相关两行改成 `go` 即可。

### 路线 D（备选）：Docker Hub 官方冻结版

```bash
docker pull minio/minio:latest   # = RELEASE.2025-09-07，官方 2025-10 停更但未删库
```

2026-10-30 实测仍可拉取，功能完好、多架构。比本自建版本旧约 5 个月。**若你不需要"可复现自建"这个特性，这条最快**；两条线 API/行为完全一致，应用层无感。

## 4. 怎么跑起来

```bash
docker run -d --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD='<换成强密码>' \
  -p 9000:9000 -p 9001:9001 \
  -v minio-data:/data \
  minio/minio:latest server /data --console-address ":9001"
```

- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` = S3 的 accessKey / secretKey（自定，别用默认值）
- 数据落在容器 `/data`，务必用 named volume 或 bind mount 持久化
- 验证：

```bash
docker run --rm minio/minio:latest --version   # 应输出 DEVELOPMENT.2026-02-12… 与 CommitID 7aac2a2c5b7c
curl -sf http://localhost:9000/minio/health/live && echo OK     # 200 = 活着
# 浏览器打开 http://localhost:9001 用 root 账号进控制台
```

docker compose 等价写法：

```yaml
services:
  minio:
    image: minio/minio:latest        # 或 ghcr.io/leehonne000/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: "<强密码>"
    ports: ["9000:9000", "9001:9001"]
    volumes: ["minio-data:/data"]
volumes: { minio-data: {} }
```

生产建议：9001 控制台只绑 `127.0.0.1`（防扫描），9000 视需要走 Nginx 反代；不要把 9001 直接暴露公网。

## 5. 应用怎么接

任何 S3 兼容 SDK 都能用（minio-java、boto3、aws-sdk-s3、minio-js……）。三个要素：

1. endpoint：`http://<host>:9000`（自建实例几乎都是 http）
2. credentials：上一步的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`
3. **必须用 path-style**（`http://host:9000/bucket/key`），虚拟主机风格（`bucket.host`）在裸 IP/端口或反代下会失败

浏览器直传/直下推荐**预签名 URL**（服务端签发限时 URL，前端直接读写桶）：桶设私有，SDK 的 `getPresignedObjectUrl`/`generate_presigned_url` 生成临时链接。

## 6. 踩坑清单（前人实测，务必先读）

| 坑 | 说明与解法 |
|---|---|
| **镜像里没有 shell** | `FROM scratch`，`docker exec -it minio sh` 必失败。调试请从外面做：控制台、`mc` 客户端、SDK，或 `docker logs minio`。 |
| **预签名 URL 在反代后 403** | minio-java 8.5.x 生成预签名前会先请求 `GET /{bucket}/?location=` 查区域，该请求经反代常报 `SignatureDoesNotMatch`。解法：MinioClient 显式设 **`.region("us-east-1")`**，跳过查询、纯本地签名。其他 SDK 同理（固定 region）。 |
| **path-style** | Java：`minioClient.disableVirtualStyleEndpoint()`；其他 SDK 对应选项。 |
| **反代必须透传 Host** | 预签名里含 host，Nginx 要 `proxy_set_header Host $host`，否则签名对不上。 |
| **内外网双 endpoint** | 容器间互访走内网地址（如 `http://minio:9000`）；签发给浏览器的预签名 URL 必须以**公网可达 endpoint** 生成。推荐客户端拆两个：操作用内网、预签名用公网。 |
| **桶懒建** | 镜像不会自动建桶，应用首次使用时创建或运维预先建好。 |
| **元数据存原文件名** | 对象自定义元数据 `x-amz-meta-*` 可用（如 originalName），读取时从响应头取回。 |
| **ARM 架构** | 本镜像只有 amd64。ARM 机器：路线 C 构建时改 `GOARCH=arm64`，或走路线 D（官方冻结版是多架构）。 |

## 7. 备份与运维

- 数据全在 `/data` 卷里：备份 = 停容器（或热备）后 `tar` 整个卷；恢复 = 解回卷再起
- 无分布式依赖，单容器即可；升级镜像换 tag 重启即可，数据在卷里不动
- 日志走 `docker logs`（MinIO 前台进程，stderr 输出）

## 8. 许可与合规（AGPL-3.0）

MinIO 是 AGPL-3.0：**自用完全没问题**；若你把镜像公开分发（推公共仓库/公开下载），需同时提供**对应源码**。本镜像的合规出口已备好：

- 源码出处：`https://github.com/minio/minio` @ commit `7aac2a2c5b7c`（已写入镜像 label `org.opencontainers.image.source`）
- 构建脚本：`build.sh` + `Dockerfile`（路线 C 提到的两个文件，随仓库公开）

只要分发时保留这两项，即满足要求。应用代码（闭源也行）调用 MinIO 的网络 API 不触发传染，传染的是"分发 MinIO 本体"这个动作。

## 9. 原机器资产索引（需要回到原环境时用）

| 资产 | 位置 |
|---|---|
| 构建配方 | 原仓库 `temp/minio-image/build.sh` + `Dockerfile`（.gitignore 挡住二进制/源码包等产物） |
| 本地镜像 | 原机器 Docker Desktop 内 `minio/minio:latest`（约 150MB；Docker Desktop 当前可能未启动，启动后 `docker images --digests` 可看完整 digest） |
| 编译产物 | `temp/minio-image/minio`（109MB Linux 二进制）、`minio-src.zip`（源码包，25MB）、`go1.27.3.windows-amd64.zip`（工具链） |
| 版本完整戳 | `DEVELOPMENT.2026-02-12T20-18-48Z` / CommitID `7aac2a2c5b7c` |

---

**一句话**：`minio/minio:latest` 是官方源码（`7aac2a2c5b7c`）自建的纯净 MinIO，scratch 打包、150MB、S3 兼容；跑法和官方镜像一模一样，接应用时记住"path-style + 固定 region us-east-1 + 反代透传 Host"三条即可。
