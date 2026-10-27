package com.leehonne.examtimeline.info.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ArticleListItemResponse {

    private Long id;

    private String title;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀）——2026-10-27 info 界面标识作者 */
    private String authorName;

    /** 封面预签名 URL，无封面为 null */
    private String coverUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
}
