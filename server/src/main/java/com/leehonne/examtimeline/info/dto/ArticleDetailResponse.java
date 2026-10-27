package com.leehonne.examtimeline.info.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 文章详情（契约 §9.3） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDetailResponse {

    private Long id;

    private String title;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀） */
    private String authorName;

    /** 正文（纯文本/Markdown 原文） */
    private String content;

    /** 封面预签名 URL，无封面为 null */
    private String coverUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
}
