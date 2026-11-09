package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 单条留言（契约 §9.4）：senderRole 0=用户 1=管理员/超管 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MessageItemVO")
public class MessageItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "senderRole")
    private Integer senderRole;

    @Schema(description = "content")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "createdAt")
    private LocalDateTime createdAt;

    /** 发送者显示名（气泡旁昵称；解析口径见 MessageService，随资料实时取） */
    @Schema(description = "senderName")
    private String senderName;

    /** 发送者头像地址（空头像 → /avatar-default.png；MinIO 对象 → 预签名 URL） */
    @Schema(description = "senderAvatar")
    private String senderAvatar;
}
