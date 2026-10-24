package com.leehonne.examtimeline.auth;

import com.leehonne.examtimeline.auth.dto.*;
import com.leehonne.examtimeline.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：发验证码 / 注册激活 / 登录 / 找回密码。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 发验证码（purpose: 1=注册激活 2=找回密码） */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
        authService.sendCode(req);
        return ApiResponse.ok();
    }

    /** 注册：校验激活码 → 建号（已激活）+ 录入科目组合，返回 token */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    /** 登录：邮箱+密码 → JWT */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    /** 找回密码：邮箱+验证码+新密码 */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ApiResponse.ok();
    }
}
