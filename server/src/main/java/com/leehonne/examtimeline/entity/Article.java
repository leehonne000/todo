package com.leehonne.examtimeline.entity;

import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.enums.ArticleStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 博客文章（t_article，2026-10-24）—— 信息模块博客内容区（§5.1）。
 * 超级管理员发布，普通用户只读；草稿(0)/发布(1) 两态，下线=改回草稿。
 */
@Getter
@Setter
@Entity
@Table(name = "t_article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 作者 = 超级管理员（t_user.role=1，应用层校验） */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 文章标题（列表页展示） */
    @Column(nullable = false, length = 120)
    private String title;

    /** 正文（v1 纯文本/Markdown 原文） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 封面图 MinIO 对象键（可选） */
    @Column(name = "cover_object_key", length = 255)
    private String coverObjectKey;

    /** 状态：0=草稿（仅管理员） 1=已发布（全员可见） */
    @Convert(converter = ArticleStatusConverter.class)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.DRAFT;

    /** 发布时间（首次发布写入，列表倒序依据）；草稿为空 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
