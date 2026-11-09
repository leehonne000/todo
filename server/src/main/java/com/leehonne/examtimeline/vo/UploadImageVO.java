package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 图片上传响应（契约 §5.5）：objectKey 为临时键（user_{uid}/tmp/...），
 * Done 落库时由后端归档到条目名下；previewUrl 前端直接渲染缩略图。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UploadImageVO")
public class UploadImageVO {

    @Schema(description = "objectKey")
    private String objectKey;

    @Schema(description = "contentType")
    private String contentType;

    @Schema(description = "sizeBytes")
    private long sizeBytes;

    /** 签名预览 URL（短时效），相对路径 */
    @Schema(description = "previewUrl")
    private String previewUrl;
}
