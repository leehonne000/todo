package com.leehonne.examtimeline.storage;

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
public class UploadImageResponse {

    private String objectKey;

    private String contentType;

    private long sizeBytes;

    /** 签名预览 URL（短时效），相对路径 */
    private String previewUrl;
}
