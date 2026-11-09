package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

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
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO req) {
        authService.sendCode(req);
        return Result.ok();
    }

    /** 注册：校验激活码 → 建号（已激活）+ 录入科目组合，返回 token */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO req) {
        return Result.ok(authService.register(req));
    }

    /** 登录：邮箱+密码 → JWT */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO req) {
        return Result.ok(authService.login(req));
    }

    /** 找回密码：邮箱+验证码+新密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO req) {
        authService.resetPassword(req);
        return Result.ok();
    }
}
