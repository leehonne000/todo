package com.leehonne.examtimeline.common;

import lombok.Getter;

/**
 * 业务状态码枚举（课程第 11 课 ResultCode 设计）：
 * 200=成功；1xxx=参数类；2xxx=认证授权类；3xxx=业务冲突类；5xxx=服务端。
 * 码值与 docs/接口文档.md、前端 api 层、verify 脚本约定一致，改动需三端同步。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(1001, "参数错误"),
    METHOD_NOT_ALLOWED(1002, "请求方法不支持"),
    UNAUTHORIZED(2001, "未登录或登录已过期，请先登录"),
    FORBIDDEN(2002, "没有权限访问该资源"),
    CONFLICT(3001, "资源冲突"),
    NOT_FOUND(3002, "资源不存在"),
    TOO_MANY_REQUESTS(3003, "请求太频繁"),
    SERVER_ERROR(5000, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
