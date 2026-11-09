package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

import com.leehonne.examtimeline.utils.ImageCompressor;
import com.leehonne.examtimeline.utils.ImageCompressor.CompressedImage;
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

    private final StorageService storage;
    private final ImageCompressor imageCompressor;

    /** 上传图片（multipart 字段名 file，≤10MB） */
    @PostMapping("/api/uploads/images")
    public Result<UploadImageVO> upload(@RequestParam("file") MultipartFile file) {
        Long userId = CurrentUserContext.currentUserId();
        CompressedImage img = imageCompressor.compress(file);
        String objectKey = "user_" + userId + "/tmp/" + UUID.randomUUID() + img.extension();
        storage.put(objectKey, img.bytes(), img.contentType(), file.getOriginalFilename());
        UploadImageVO resp = new UploadImageVO(
                objectKey, img.contentType(), img.bytes().length, storage.signedUrl(objectKey));
        return Result.ok(resp);
    }
}
