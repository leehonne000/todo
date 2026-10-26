package com.leehonne.examtimeline.storage;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.storage.ImageCompressor.CompressedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 图片上传（契约 §5.5，记录图片/文章封面共用）。
 * 压缩后写 MinIO 私有桶临时键 user_{uid}/tmp/{uuid}.{ext}，返回 SDK 预签名 GET URL；
 * Done 落库时由 LearnService 归档到条目名下。读取不经后端——预签名 URL 直达 MinIO。
 */
@RestController
@RequiredArgsConstructor
public class UploadController {

    private final MinioStorageService storage;
    private final ImageCompressor imageCompressor;

    /** 上传图片（multipart 字段名 file，≤10MB） */
    @PostMapping("/api/uploads/images")
    public ApiResponse<UploadImageResponse> upload(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.currentUserId();
        CompressedImage img = imageCompressor.compress(file);
        String objectKey = "user_" + userId + "/tmp/" + UUID.randomUUID() + img.extension();
        storage.put(objectKey, img.bytes(), img.contentType(), file.getOriginalFilename());
        UploadImageResponse resp = new UploadImageResponse(
                objectKey, img.contentType(), img.bytes().length, storage.signedUrl(objectKey));
        return ApiResponse.ok(resp);
    }
}
