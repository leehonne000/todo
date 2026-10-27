package com.leehonne.examtimeline.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 管理文章列表条目（契约 §11.1） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminArticleListItemResponse {

    private Long id;

    private String title;

    /** 状态：0=草稿 1=已发布 */
    private Integer status;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀）——2026-10-27 文章归属权标识 */
    private String authorName;

    /** 作者用户 id（前端判定可操作性：本人或超管才能动） */
    private Long authorId;

    /** 首次发布时间；从未发布的草稿为 null */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
