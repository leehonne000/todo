package com.leehonne.examtimeline.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录详情（契约 §7.2，与前端 mock 的 entryDetail 对齐）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntryDetailResponse {

    private Long id;

    private String subjectName;

    /** 1=note 2=错题 */
    private Integer type;

    /** 文字内容（纯图片记录为空串） */
    private String content;

    private List<String> tags;

    private List<DetailImage> images;

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
    public static class DetailImage {

        private String objectKey;

        /** 预签名 GET（短时效），img.src 直用 */
        private String previewUrl;
    }
}
