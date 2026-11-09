package com.leehonne.examtimeline.common.context;

import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.common.ResultCode;

/**
 * 当前登录用户上下文（基于 ThreadLocal，课程第 16 课 CurrentUserContext）。
 * LoginInterceptor 在 preHandle 中存入，Controller/Service 中取出，afterCompletion 中移除（防线程复用泄漏）。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(LoginUser user) {
        CURRENT_USER.set(user);
    }

    public static LoginUser get() {
        return CURRENT_USER.get();
    }

    /** 当前登录用户 ID；未登录抛业务异常（拦截器已挡 401，此处兜底） */
    public static Long currentUserId() {
        LoginUser user = CURRENT_USER.get();
        if (user == null || user.getUserId() == null) {
            throw BusinessException.unauthorized(ResultCode.UNAUTHORIZED.getMessage());
        }
        return user.getUserId();
    }

    public static void remove() {
        CURRENT_USER.remove();
    }
}
