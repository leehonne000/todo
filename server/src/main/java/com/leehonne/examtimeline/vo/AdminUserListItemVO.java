package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理用户列表条目（契约 §11.7）——提拔入口。
 * 不返回密码哈希；nickname 为 2026-10-27 增补（列表展示、按昵称定位）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AdminUserListItemVO")
public class AdminUserListItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "email")
    private String email;

    @Schema(description = "nickname")
    private String nickname;

    /** 角色：0=普通用户 1=超级管理员 */
    @Schema(description = "role")
    private Integer role;

    @Schema(description = "examYear")
    private Integer examYear;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "createdAt")
    private LocalDateTime createdAt;
}
