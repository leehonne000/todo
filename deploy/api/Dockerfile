# Big Tree 后端镜像：本地打好 jar 拷入（服务器无需 Maven/JDK）
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY exam-timeline.jar app.jar
EXPOSE 8080
# 4G 内存机器：JVM 堆上限 1200MB（MySQL/MinIO/Nginx 分剩下的）
ENTRYPOINT ["java", "-Xms256m", "-Xmx1200m", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
