package com.leehonne.examtimeline.enums;

/**
 * 用户角色（t_user.role）：0=普通用户，1=超级管理员（管理信息模块博客，§5.1）。
 */
public enum UserRole {
    USER(0),
    SUPER_ADMIN(1);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserRole fromCode(int code) {
        for (UserRole r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("未知 user role: " + code);
    }
}
