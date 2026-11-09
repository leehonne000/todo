package com.leehonne.examtimeline.service;

/**
 * 对象存储抽象（契约 §4.4/§5.5/§10）。
 *
 * <p>实现为 MinIO 私有桶（见 {@link com.leehonne.examtimeline.service.impl.MinioStorageServiceImpl}）；objectKey 契约与 MinIO 一致
 * （{@code user_{uid}/entry_{eid}/{uuid}.{ext}}，上传期临时键 {@code user_{uid}/tmp/...}），
 * 之后切 MinIO 私有桶只换实现类，业务层不动。
 */
public interface StorageService {

    /** 对象元数据（t_entry_image 落库用） */
    record ObjectStat(String objectKey, String contentType, long sizeBytes, String originalName) {
    }

    /** 读取对象元数据（大小/类型/原始名） */
    ObjectStat stat(String objectKey);

    /** 写入对象（覆盖写）；originalName 记入元数据（可空） */
    void put(String objectKey, byte[] content, String contentType, String originalName);

    /** 移动对象（Done 时把 tmp 临时键归档到条目名下）；元数据随行 */
    void move(String fromKey, String toKey);

    boolean exists(String objectKey);

    /**
     * 签名预览 URL（短时效）。
     * 鉴权走签名而非 JWT——{@code <img src>} 带不了 Authorization 头。
     */
    String signedUrl(String objectKey);

    /** 读取对象字节 */
    byte[] read(String objectKey);

    void delete(String objectKey);
}
