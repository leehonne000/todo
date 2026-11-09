package com.leehonne.examtimeline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置（课程第 15 课 SecurityConfig 精简版）：
 * 只引 spring-security-crypto 注册 BCryptPasswordEncoder，不启用 Security 过滤链；
 * 登录校验统一走 LoginInterceptor（HandlerInterceptor）。
 */
@Configuration
public class SecurityConfig {

    /**
     * BCryptPasswordEncoder：strength（成本因子）默认 10，值越大越安全但越慢。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
