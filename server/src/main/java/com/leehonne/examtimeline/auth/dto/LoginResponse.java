package com.leehonne.examtimeline.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** 登录成功响应 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String email;
}
