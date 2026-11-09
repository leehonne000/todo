package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 管理端会话详情（契约 §11.15）：某文章下与某用户的完整对话串（时间升序） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AdminThreadDetailVO")
public class AdminThreadDetailVO {

    @Schema(description = "articleId")
    private Long articleId;

    @Schema(description = "userId")
    private Long userId;

    /** 文章作者（会话归属管理员）：前端据此显示回复框——仅作者本人可回复（超管可看不可代回） */
    @Schema(description = "authorId")
    private Long authorId;

    @Schema(description = "articleTitle")
    private String articleTitle;

    @Schema(description = "authorName")
    private String authorName;

    @Schema(description = "userName")
    private String userName;

    @Schema(description = "userEmail")
    private String userEmail;

    /** 留言用户头像地址（对话区头部展示） */
    @Schema(description = "userAvatar")
    private String userAvatar;

    @Schema(description = "messages")
    private List<MessageItemVO> messages;
}
