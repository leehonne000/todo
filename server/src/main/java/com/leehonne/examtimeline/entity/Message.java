package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 留言（t_message）—— 文章评论区；user↔管理员 回复串（§9）。
 * sender_role：0=普通用户 1=管理员。
 */
@Getter
@Setter
@TableName("t_message")
@Schema(description = "留言实体")
public class Message {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "留言用户ID")
    private Long userId;

    @Schema(description = "发送者ID（回复方）")
    private Long senderId;

    @Schema(description = "发送者角色：0=普通用户 1=管理员")
    private Integer senderRole;

    @Schema(description = "留言内容")
    private String content;

    @Schema(description = "对方是否已读")
    private Boolean isRead = false;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
