package com.leehonne.examtimeline.auth.dto;

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
public class LoginResponse {

    private String token;
    private Long userId;
    private String email;
    private Integer role;
}
