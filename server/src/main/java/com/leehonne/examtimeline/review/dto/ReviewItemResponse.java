package com.leehonne.examtimeline.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 到期复习项（契约 §6.1）：Review 卡沉浸式复习的一条内容。
 * 字段口径与前端 mock 的 entryDetail 对齐（多带 reviewStatus）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewItemResponse {

    private Long id;

    private String subjectName;

    /** 1=note 2=错题 */
    private Integer type;

    /** 文字内容（纯图片记录为空串） */
    private String content;

    private List<String> tags;

    private List<ReviewImage> images;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 已完成轮数 0~6 */
    private Integer round;

    /** 0=队列中 1=已出队 */
    private Integer reviewStatus;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewImage {

        private String objectKey;

        /** 预签名 GET（短时效），img.src 直用 */
        private String previewUrl;
    }
}
