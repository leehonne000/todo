package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 博客列表条目（契约 §9.2） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ArticleListItemVO")
public class ArticleListItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "title")
    private String title;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀）——2026-10-27 info 界面标识作者 */
    @Schema(description = "authorName")
    private String authorName;

    /** 封面预签名 URL，无封面为 null */
    @Schema(description = "coverUrl")
    private String coverUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "publishedAt")
    private LocalDateTime publishedAt;
}
