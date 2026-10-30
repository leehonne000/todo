package com.leehonne.examtimeline.storage;

import com.leehonne.examtimeline.common.BizException;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MinIO 对象存储（契约 §10）——Learn/文章封面图片链路的唯一存储后端。
 *
 * <p>私有桶 {@code exam-timeline}，读取一律走 SDK 签发的预签名 GET（短时效）；
 * objectKey 契约：上传期临时键 {@code user_{uid}/tmp/{uuid}.{ext}}，Done 归档 {@code user_{uid}/entry_{eid}/{文件名}}。
 * MinIO 没有原地改名，「移动」= 服务端 copy + 删源。原始文件名走对象 user metadata（x-amz-meta-original-name）。
 *
 * <p><b>预签名 host 与操作 host 分离（部署路线②，开发进度「二十八」）</b>：
 * 预签名是本地计算不发网络请求，签名里的 host 必须等于浏览器实际访问的地址，
 * 否则他机访问时会去请求自己/错误的 host（跨设备裂图）。因此：
 * <ul>
 *   <li>操作客户端（读写删）用 {@code app.minio.endpoint}——容器内网地址（如 http://minio:9000）；</li>
 *   <li>预签名客户端用 {@code app.minio.presign-endpoint}——公网可达地址
 *       （开发期 http://localhost:9000 / 局域网 IP，部署期经 Nginx 反代的同域/子域 HTTPS 地址）。</li>
 * </ul>
 * 未配置 presign-endpoint 时回退 endpoint（本地开发行为不变）。
 */
@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient client;
    private final MinioClient presignClient;
    private final String bucket;
    private final int presignTtlSeconds;
    private volatile boolean bucketEnsured;

    public MinioStorageService(
            @Value("${app.minio.endpoint}") String endpoint,
            @Value("${app.minio.presign-endpoint:}") String presignEndpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket}") String bucket,
            @Value("${app.minio.presign-ttl-hours:12}") long presignTtlHours) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region("us-east-1") // 固定 region：跳过 GetBucketLocation 查询（经反代时该查询签名不通）
                .build();
        this.client.disableVirtualStyleEndpoint(); // 强制路径风格（桶名进路径），兼容 Docker 内网主机名/反代
        String presignBase = (presignEndpoint == null || presignEndpoint.isBlank()) ? endpoint : presignEndpoint;
        this.presignClient = MinioClient.builder()
                .endpoint(presignBase)
                .credentials(accessKey, secretKey)
                .region("us-east-1") // 同上；预签名纯本地计算，不再发起网络查询
                .build();
        this.presignClient.disableVirtualStyleEndpoint(); // 预签名 URL 形如 {presign-endpoint}/{bucket}/{key}?X-Amz-...
        if (!presignBase.equals(endpoint)) {
            log.info("MinIO 预签名 host 与操作 host 分离: presign={} ops={}", presignBase, endpoint);
        }
        this.bucket = bucket;
        this.presignTtlSeconds = (int) (presignTtlHours * 3600);
    }

    /** 对象元数据（t_entry_image 落库用） */
    public record ObjectStat(String objectKey, String contentType, long sizeBytes, String originalName) {
    }

    /** 确保私有桶在（幂等）：启动时试一次，MinIO 晚起则首次写入时补建 */
    @PostConstruct
    synchronized void ensureBucket() {
        if (bucketEnsured) {
            return;
        }
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 桶已创建: {}", bucket);
            }
            bucketEnsured = true;
        } catch (Exception e) {
            log.warn("MinIO 桶检查失败（服务未就绪？首次写入时重试）: {}", e.getMessage());
        }
    }

    /** 写入对象（覆盖写）；originalName 记进 user metadata */
    public void put(String objectKey, byte[] content, String contentType, String originalName) {
        ensureBucket();
        Map<String, String> meta = new HashMap<>();
        if (originalName != null && !originalName.isBlank()) {
            // 文件名按 UTF-8 字节转义存（MinIO metadata 只收 ASCII 头）
            meta.put("original-name", escape(originalName));
        }
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(contentType)
                    .userMetadata(meta)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 写入失败 key={}", objectKey, e);
            throw new BizException(5000, "文件写入失败");
        }
    }

    /** 移动对象（Done 归档 tmp→entry）：MinIO 无 rename，服务端 copy 后删源 */
    public void move(String fromKey, String toKey) {
        if (!exists(fromKey)) {
            throw BizException.badRequest("图片不存在或已过期，请重新上传");
        }
        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .object(toKey)
                    .source(CopySource.builder().bucket(bucket).object(fromKey).build())
                    .build());
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(fromKey).build());
        } catch (Exception e) {
            log.error("MinIO 归档失败 {} -> {}", fromKey, toKey, e);
            throw new BizException(5000, "文件归档失败");
        }
    }

    public boolean exists(String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new BizException(5000, "文件存储访问失败");
        }
    }

    /** 对象元数据：content-type / 大小 / 原始文件名 */
    public ObjectStat stat(String objectKey) {
        try {
            StatObjectResponse st = client.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return new ObjectStat(objectKey, st.contentType(), st.size(), lookupOriginalName(st.userMetadata()));
        } catch (ErrorResponseException e) {
            throw BizException.badRequest("图片不存在或已过期，请重新上传");
        } catch (Exception e) {
            throw new BizException(5000, "文件存储访问失败");
        }
    }

    /** 预签名 GET（短时效）——等效契约里的"预签名，短时效"，前端 img.src 直用；
     *  用 presignClient 签发（host=公网可达地址），签名本地计算不发请求 */
    public String signedUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(presignTtlSeconds)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 预签名失败 key={}", objectKey, e);
            throw new BizException(5000, "图片链接签发失败");
        }
    }

    /** 读取对象字节（按需下载/导出用） */
    public byte[] read(String objectKey) {
        try (var in = client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw BizException.notFound("文件不存在");
        }
    }

    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            log.error("MinIO 删除失败 key={}", objectKey, e);
            throw new BizException(5000, "文件删除失败");
        }
    }

    // ---- 内部 ----

    /** metadata 里取原始文件名（SDK 不同版本键名可能带 x-amz-meta- 前缀，大小写也松） */
    private String lookupOriginalName(Map<String, String> userMetadata) {
        if (userMetadata == null) {
            return null;
        }
        for (Map.Entry<String, String> e : userMetadata.entrySet()) {
            String k = e.getKey().toLowerCase(Locale.ROOT);
            if (k.equals("original-name") || k.endsWith("original-name")) {
                return e.getValue() == null ? null : unescape(e.getValue());
            }
        }
        return null;
    }

    /** 原始文件名转 ASCII 安全形式：%XX 按 UTF-8 字节 */
    private String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int v = b & 0xFF;
            if ((v >= 'a' && v <= 'z') || (v >= 'A' && v <= 'Z') || (v >= '0' && v <= '9')
                    || v == '.' || v == '_' || v == '-') {
                sb.append((char) v);
            } else {
                sb.append('%').append(String.format("%02X", v));
            }
        }
        return sb.toString();
    }

    private String unescape(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.write(c);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
