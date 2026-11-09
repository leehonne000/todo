package com.leehonne.examtimeline.config;

import com.leehonne.examtimeline.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（课程第 16 课 WebMvcConfig）：注册登录拦截器与放行路径。
 * 放行：认证接口、接口文档、健康检查；其余请求须带合法 JWT。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/**",          // 发码/注册/登录/找回密码
                        "/actuator/**",          // 健康检查（容器 HEALTHCHECK）
                        "/doc.html",             // Knife4j 文档
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error"
                );
    }

    /** 开发期放行跨域（浏览器测试页 file:// 直连 API 用）；上线前收紧为正式域名 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
