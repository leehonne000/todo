package com.leehonne.examtimeline.common.context;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 当前登录用户载体（课程第 16 课 LoginUser）—— 拦截器解析 JWT 后写入 CurrentUserContext。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户")
public class LoginUser {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录邮箱")
    private String email;

    @Schema(description = "角色：0=普通用户 1=管理员 2=超级管理员")
    private Integer role;
}
