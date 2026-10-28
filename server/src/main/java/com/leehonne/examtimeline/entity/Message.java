package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章留言（t_message，2026-10-28）—— 信息模块「读者来信」（§9.4）：
 * 文章下的一对一私密留言通道（非公开评论区）。会话 = (articleId, userId) 多轮对话串。
 * 可见性：仅会话双方 + 超管；管理员之间互不可见、用户之间互不可见。
 */
@Getter
@Setter
@Entity
@Table(name = "t_message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属文章（会话锚点）；文章删除留言随之级联删除 */
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    /** 会话中的用户方：senderRole=0 时是发送者，senderRole=1 时是收件人 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 真实发送者（t_user.id，冗余）：气泡头像/昵称解析用。历史数据可空，回退口径见 MessageService */
    @Column(name = "sender_id")
    private Long senderId;

    /** 发送方：0=用户 1=管理员/超管（回复） */
    @Column(name = "sender_role", nullable = false)
    private Integer senderRole;

    /** 留言正文（1~300 字，应用层校验） */
    @Column(nullable = false, length = 300)
    private String content;

    /** 对方已读：0=未读（红点） 1=已读（用户读回复 / 管理员读留言后置 1） */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /** 发送时间（会话排序依据） */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
