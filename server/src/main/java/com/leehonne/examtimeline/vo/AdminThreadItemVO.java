package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "AdminThreadItemVO")
public class AdminThreadItemVO {

    /** 会话定位（后续取详情/回复用） */
    @Schema(description = "articleId")
    private Long articleId;

    @Schema(description = "userId")
    private Long userId;

    @Schema(description = "articleTitle")
    private String articleTitle;

    /** 文章作者（会话归属的管理员；超管全局视图区分用） */
    @Schema(description = "authorId")
    private Long authorId;

    @Schema(description = "authorName")
    private String authorName;

    /** 留言用户显示名（昵称优先，回退邮箱 @ 前缀）+ 邮箱 */
    @Schema(description = "userName")
    private String userName;

    @Schema(description = "userEmail")
    private String userEmail;

    /** 留言用户头像地址（会话列表头像） */
    @Schema(description = "userAvatar")
    private String userAvatar;

    /** 最新一条留言摘要 */
    @Schema(description = "lastContent")
    private String lastContent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "lastAt")
    private LocalDateTime lastAt;

    /** 最新一条是谁发的：0=用户 1=管理员 */
    @Schema(description = "lastFrom")
    private Integer lastFrom;

    /** 本会话内用户发来、管理员未读的条数（红点/加粗判定） */
    @Schema(description = "unreadCount")
    private long unreadCount;

    @Schema(description = "messageCount")
    private long messageCount;
}
