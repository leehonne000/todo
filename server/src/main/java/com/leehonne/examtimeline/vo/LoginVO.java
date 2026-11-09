package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录成功响应。
 * role（2026-10-27 增补）：0=普通用户 1=超级管理员——管理后台据此做入口门禁，
 * 普通用户端也可据此决定是否展示"管理后台"入口（权限叠加，§11.2）。
 */
@Getter
@Setter
@AllArgsConstructor
@Schema(description = "LoginVO")
public class LoginVO {

    @Schema(description = "token")
    private String token;
    @Schema(description = "userId")
    private Long userId;
    @Schema(description = "email")
    private String email;
    @Schema(description = "role")
    private Integer role;
}
