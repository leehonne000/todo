package com.leehonne.examtimeline.service.impl;

import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 邮件发送实现（QQ SMTP）。发验证码用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerifyCode(String to, String code, String scene) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "Big Tree");
            helper.setTo(to);
            helper.setSubject("【Big Tree】" + scene + "验证码");
            helper.setText(buildHtml(code, scene), true);
            mailSender.send(message);
            log.info("验证码邮件已发送 to={} scene={}", to, scene);
        } catch (Exception e) {
            log.error("验证码邮件发送失败 to={}", to, e);
            throw BusinessException.badRequest("邮件发送失败，请稍后重试");
        }
    }

    private String buildHtml(String code, String scene) {
        return "<div style=\"font-family:sans-serif;padding:24px;color:#333\">"
                + "<h2 style=\"margin:0 0 4px;color:#2F4A1E\">🌳 Big Tree</h2>"
                + "<p style=\"margin:0 0 16px;color:#7A8B6B;font-size:13px;letter-spacing:1px\">考研时间和知识点规划助手</p>"
                + "<p>你正在" + scene + "，验证码为：</p>"
                + "<p style=\"font-size:28px;letter-spacing:6px;font-weight:bold;color:#559238\">" + code + "</p>"
                + "<p style=\"color:#888\">10 分钟内有效。若非本人操作，请忽略本邮件。</p>"
                + "</div>";
    }
}
