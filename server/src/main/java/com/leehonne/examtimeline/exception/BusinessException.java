package com.leehonne.examtimeline.exception;

import com.leehonne.examtimeline.common.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常（课程第 13 课 BusinessException）—— Service 层抛出，由全局异常处理器统一转 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    // ---- 常用工厂（与错误码分层：1xxx 参数 / 2xxx 认证 / 3xxx 业务）----

    public static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ResultCode.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ResultCode.FORBIDDEN, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ResultCode.CONFLICT, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND, message);
    }

    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(ResultCode.TOO_MANY_REQUESTS, message);
    }
}
