package com.leehonne.examtimeline.message.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 管理端留言会话列表条目（契约 §11.14）：一个会话 = 某文章下某用户的一段对话 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminThreadItemResponse {

    /** 会话定位（后续取详情/回复用） */
    private Long articleId;

    private Long userId;

    private String articleTitle;

    /** 文章作者（会话归属的管理员；超管全局视图区分用） */
    private Long authorId;

    private String authorName;

    /** 留言用户显示名（昵称优先，回退邮箱 @ 前缀）+ 邮箱 */
    private String userName;

    private String userEmail;

    /** 留言用户头像地址（会话列表头像） */
    private String userAvatar;

    /** 最新一条留言摘要 */
    private String lastContent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastAt;

    /** 最新一条是谁发的：0=用户 1=管理员 */
    private Integer lastFrom;

    /** 本会话内用户发来、管理员未读的条数（红点/加粗判定） */
    private long unreadCount;

    private long messageCount;
}
