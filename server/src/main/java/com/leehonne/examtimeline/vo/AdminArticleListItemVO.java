package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "AdminArticleListItemVO")
public class AdminArticleListItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "title")
    private String title;

    /** 状态：0=草稿 1=已发布 */
    @Schema(description = "status")
    private Integer status;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀）——2026-10-27 文章归属权标识 */
    @Schema(description = "authorName")
    private String authorName;

    /** 作者用户 id（前端判定可操作性：本人或超管才能动） */
    @Schema(description = "authorId")
    private Long authorId;

    /** 首次发布时间；从未发布的草稿为 null */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "publishedAt")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "updatedAt")
    private LocalDateTime updatedAt;
}
