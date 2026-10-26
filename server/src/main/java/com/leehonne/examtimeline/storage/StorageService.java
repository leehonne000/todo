package com.leehonne.examtimeline.storage;

/**
 * 对象存储抽象（契约 §4.4/§5.5/§10）。
 *
 * <p>v1 用本地磁盘实现（见 {@link LocalStorageService}）；objectKey 契约与 MinIO 一致
 * （{@code user_{uid}/entry_{eid}/{uuid}.{ext}}，上传期临时键 {@code user_{uid}/tmp/...}），
 * 之后切 MinIO 私有桶只换实现类，业务层不动。
 */
public interface StorageService {

    /** 写入对象（覆盖写）；originalName 记入元数据（可空） */
    void put(String objectKey, byte[] content, String contentType, String originalName);

    /** 移动对象（Done 时把 tmp 临时键归档到条目名下）；元数据随行 */
    void move(String fromKey, String toKey);

    boolean exists(String objectKey);

    /** 上传时的原始文件名（纯观赏字段，可空） */
    String originalName(String objectKey);

    /** 对象 MIME 类型（写入时记录，预签名回填 Content-Type 用） */
    String contentType(String objectKey);

    /** 对象大小（字节） */
    long size(String objectKey);

    /**
     * 签名预览 URL（相对路径，短时效）。
     * 鉴权走签名而非 JWT——{@code <img src>} 带不了 Authorization 头。
     */
    String signedUrl(String objectKey);

    /** 校验签名 URL（取图接口用）；过期或签名不符返回 false */
    boolean verifySignedUrl(String objectKey, long exp, String sig);

    /** 读取对象字节（文件服务用） */
    byte[] read(String objectKey);

    void delete(String objectKey);
}
