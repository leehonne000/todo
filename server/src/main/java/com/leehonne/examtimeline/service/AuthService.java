package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.dto.LoginDTO;
import com.leehonne.examtimeline.dto.RegisterDTO;
import com.leehonne.examtimeline.dto.ResetPasswordDTO;
import com.leehonne.examtimeline.dto.SendCodeDTO;
import com.leehonne.examtimeline.vo.LoginVO;

/**
 * 注册登录业务：发码 / 注册激活 / 登录 / 找回密码。
 * 规则（docs/数据库设计.md §2.9）：验证码 10 分钟有效、只用一次、同邮箱同用途 60s 限频、purpose 防串用。
 */
public interface AuthService {

    void sendCode(SendCodeDTO req);

    LoginVO register(RegisterDTO req);

    LoginVO login(LoginDTO req);

    void resetPassword(ResetPasswordDTO req);
}
