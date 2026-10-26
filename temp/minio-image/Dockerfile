# 纯 MinIO 镜像（自建，官方源码编译）——成品盒里只有 minio 二进制 + CA 证书
# 配方说明见 temp/minio-image/build.sh（可复现构建）
FROM scratch

LABEL org.opencontainers.image.title="minio" \
      org.opencontainers.image.description="Pure MinIO object storage (built from official source)" \
      org.opencontainers.image.source="https://github.com/minio/minio" \
      org.opencontainers.image.version="v0.0.0-20260212201848-7aac2a2c5b7c"

COPY minio /usr/bin/minio
COPY ca-certificates.crt /etc/ssl/certs/ca-certificates.crt

EXPOSE 9000 9001

ENTRYPOINT ["/usr/bin/minio"]
CMD ["server", "/data"]
