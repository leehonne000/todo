package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 图片元数据（t_entry_image）—— 本体在 MinIO 私有桶 exam-timeline，本表只存指路牌。
 * 一条记录最多 10 张（应用层校验）。
 */
@Getter
@Setter
@Entity
@Table(name = "t_entry_image")
public class EntryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属记录项 */
    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    /** 归属用户（鉴权免回表） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** MinIO 对象键：user_{uid}/entry_{eid}/{uuid}.webp */
    @Column(name = "object_key", nullable = false, unique = true, length = 255)
    private String objectKey;

    /** 上传时原始文件名（展示/下载） */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /** MIME 类型（如 image/webp），预签名 GET 回填 Content-Type */
    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    /** 压缩后对象大小（容量统计/排错） */
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    /** 缩略图顺序 0~9 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
