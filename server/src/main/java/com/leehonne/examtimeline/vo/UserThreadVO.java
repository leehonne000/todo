package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 用户视角的留言会话（契约 §9.5）：某文章下自己 ↔ 文章作者管理员的对话串 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UserThreadVO")
public class UserThreadVO {

    @Schema(description = "articleId")
    private Long articleId;

    @Schema(description = "title")
    private String title;

    /** 文章作者显示名（留言对象 = 该管理员） */
    @Schema(description = "authorName")
    private String authorName;

    /** 时间升序的对话串（可能为空 = 还没留言过） */
    @Schema(description = "messages")
    private List<MessageItemVO> messages;
}
