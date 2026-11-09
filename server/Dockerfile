# ============================================================
# Big Tree 后端镜像（课程第 07/08 课多阶段构建规范）
# 阶段 1 builder：maven:3.8-openjdk-17 —— pom 先行缓存依赖，再编译打包
# 阶段 2 runtime：eclipse-temurin:17-jre-alpine —— 非 root、tzdata、HEALTHCHECK
# 构建（在仓库根或 deploy/ 下）：docker build -t exam-timeline-api ./server
# ============================================================

# ============ 阶段 1：构建阶段（命名 builder）============
FROM maven:3.8-openjdk-17 AS builder

WORKDIR /build

# Maven 阿里云镜像（课程 K3-3）：大陆服务器拉依赖更快更稳
COPY settings.xml /root/.m2/settings.xml

# 层缓存优化：先拷 pom 单独下依赖（依赖不变时不重复下载）
COPY pom.xml .
RUN mvn -q dependency:go-offline

# 再拷源码编译打包
COPY src ./src
RUN mvn -q clean package -DskipTests \
 && mv target/*.jar /build/app.jar

# ============ 阶段 2：运行阶段（最终镜像）============
FROM eclipse-temurin:17-jre-alpine

# 时区 Asia/Shanghai（合并 RUN，装完 tzdata 即清）
RUN apk add --no-cache curl tzdata \
 && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
 && echo "Asia/Shanghai" > /etc/timezone \
 && apk del tzdata

WORKDIR /app

# 从构建阶段拷贝产物
COPY --from=builder /build/app.jar /app/app.jar

# 创建非 root 用户运行（安全加固）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 声明性指令
EXPOSE 8080
VOLUME ["/app/logs"]

# 运行时环境变量：4G 内存机器堆上限 1200MB（MySQL/MinIO/Nginx 分剩下的）
ENV SPRING_PROFILES_ACTIVE=prod \
 JAVA_OPTS="-Xms256m -Xmx1200m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 入口点 + 默认参数（CMD 可被 docker run 参数覆盖）
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar \"$@\"","--"]
CMD ["--spring.profiles.active=prod"]

# 健康检查（Actuator，登录拦截器已放行 /actuator/**）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
 CMD curl -f http://localhost:8080/actuator/health || exit 1
