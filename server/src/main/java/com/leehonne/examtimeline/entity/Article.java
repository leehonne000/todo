package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leehonne.examtimeline.enums.ArticleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 博客文章（t_article，2026-10-24）—— 信息模块博客内容区（§5.1）。
 * 超级管理员发布，普通用户只读；草稿(0)/发布(1) 两态，下线=改回草稿。
 */
@Getter
@Setter
@TableName("t_article")
@Schema(description = "文章实体")
public class Article {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 作者 = 超级管理员（t_user.role=1，应用层校验） */
    @Schema(description = "作者ID")
    private Long authorId;

    /** 文章标题（列表页展示） */
    @Schema(description = "标题")
    private String title;

    /** 正文（v1 纯文本/Markdown 原文） */
    @Schema(description = "正文")
    private String content;

    /** 封面图 MinIO 对象键（可选） */
    @Schema(description = "封面图对象键")
    private String coverObjectKey;

    /** 状态：0=草稿（仅管理员） 1=已发布（全员可见） */
    @Schema(description = "状态：0=草稿 1=已发布")
    private ArticleStatus status = ArticleStatus.DRAFT;

    /** 发布时间（首次发布写入，列表倒序依据）；草稿为空 */
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
