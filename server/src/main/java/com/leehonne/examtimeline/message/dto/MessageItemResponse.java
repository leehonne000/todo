package com.leehonne.examtimeline.message.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class MessageItemResponse {

    private Long id;

    private Integer senderRole;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 发送者显示名（气泡旁昵称；解析口径见 MessageService，随资料实时取） */
    private String senderName;

    /** 发送者头像地址（空头像 → /avatar-default.png；MinIO 对象 → 预签名 URL） */
    private String senderAvatar;
}
