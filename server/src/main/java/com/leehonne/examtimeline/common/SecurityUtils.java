package com.leehonne.examtimeline.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户：JwtAuthFilter 把 userId（Long）放进 principal。
 * SecurityConfig 已把未登录挡在 401，这里兜底再防一手。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id;
        }
        throw BizException.unauthorized("未登录或登录已过期，请先登录");
    }
}
