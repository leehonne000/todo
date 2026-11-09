package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "ReviewItemVO")
public class ReviewItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "subjectName")
    private String subjectName;

    /** 1=note 2=错题 */
    @Schema(description = "type")
    private Integer type;

    /** 文字内容（纯图片记录为空串） */
    @Schema(description = "content")
    private String content;

    @Schema(description = "tags")
    private List<String> tags;

    @Schema(description = "images")
    private List<ReviewImage> images;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "createdAt")
    private LocalDateTime createdAt;

    /** 已完成轮数 0~6 */
    @Schema(description = "round")
    private Integer round;

    /** 0=队列中 1=已出队 */
    @Schema(description = "reviewStatus")
    private Integer reviewStatus;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewImage {

        @Schema(description = "objectKey")
        private String objectKey;

        /** 预签名 GET（短时效），img.src 直用 */
        @Schema(description = "previewUrl")
        private String previewUrl;
    }
}
