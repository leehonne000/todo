package com.leehonne.examtimeline.message.dto;

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
public class AdminThreadDetailResponse {

    private Long articleId;

    private Long userId;

    /** 文章作者（会话归属管理员）：前端据此显示回复框——仅作者本人可回复（超管可看不可代回） */
    private Long authorId;

    private String articleTitle;

    private String authorName;

    private String userName;

    private String userEmail;

    /** 留言用户头像地址（对话区头部展示） */
    private String userAvatar;

    private List<MessageItemResponse> messages;
}
