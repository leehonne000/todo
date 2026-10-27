package com.leehonne.examtimeline.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理文章详情（契约 §11.9 增补）——编辑表单回显用：
 * 含草稿、含 coverObjectKey（整体替换式保存时原样回传表示封面不变）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminArticleDetailResponse {

    private Long id;

    private String title;

    /** 正文（纯文本/Markdown 原文） */
    private String content;

    /** 封面对象键（编辑回传用），无封面为 null */
    private String coverObjectKey;

    /** 封面预签名 URL（预览用），无封面为 null */
    private String coverUrl;

    /** 状态：0=草稿 1=已发布 */
    private Integer status;

    /** 作者显示名（昵称，缺省邮箱 @ 前缀） */
    private String authorName;

    /** 作者用户 id */
    private Long authorId;

    /** 当前登录者可否编辑/发布/删除（本人或超管 true）——2026-10-27 文章归属权 */
    private Boolean editable;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
