package com.leehonne.examtimeline.common;

import lombok.Getter;

/**
 * 业务异常：带错误码（1xxx 参数 / 2xxx 认证 / 3xxx 业务），由全局异常处理器统一转响应。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    // ---- 常用错误码 ----
    public static BizException badRequest(String message) {
        return new BizException(1001, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(2001, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(2002, message);
    }

    public static BizException conflict(String message) {
        return new BizException(3001, message);
    }

    public static BizException notFound(String message) {
        return new BizException(3002, message);
    }

    public static BizException tooManyRequests(String message) {
        return new BizException(3003, message);
    }
}
