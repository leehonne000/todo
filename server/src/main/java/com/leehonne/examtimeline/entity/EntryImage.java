package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 图片元数据（t_entry_image）—— 本体在 MinIO 私有桶 exam-timeline，本表只存指路牌。
 * 一条记录最多 10 张（应用层校验）。
 */
@Getter
@Setter
@TableName("t_entry_image")
@Schema(description = "记录项图片元数据实体")
public class EntryImage {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 所属记录项 */
    @Schema(description = "记录项ID")
    private Long entryId;

    /** 归属用户（鉴权免回表） */
    @Schema(description = "用户ID")
    private Long userId;

    /** MinIO 对象键：user_{uid}/entry_{eid}/{uuid}.webp */
    @Schema(description = "MinIO 对象键")
    private String objectKey;

    /** 上传时原始文件名（展示/下载） */
    @Schema(description = "原始文件名")
    private String originalName;

    /** MIME 类型（如 image/webp），预签名 GET 回填 Content-Type */
    @Schema(description = "MIME 类型")
    private String contentType;

    /** 压缩后对象大小（容量统计/排错） */
    @Schema(description = "对象大小（字节）")
    private Integer sizeBytes;

    /** 缩略图顺序 0~9 */
    @Schema(description = "缩略图顺序 0~9")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
