package com.leehonne.examtimeline.service;

/**
 * 邮件发送服务（QQ SMTP）—— 注册激活/找回密码验证码邮件。
 */
public interface MailService {

    /** 发送验证码邮件（同步；调用方在事务外，失败即报错给用户） */
    void sendVerifyCode(String to, String code, String scene);
}
