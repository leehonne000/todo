package com.leehonne.examtimeline.enums;

/**
 * 用户角色（t_user.role）三级制（2026-10-27）：
 * 0=普通用户；1=管理员（可写自己的文章）；2=超级管理员（admin@bigtree.com，管全部管理员与内容）。
 * 口径：管理员之间互不干涉（不能改对方文章、不能动对方权限）；超管可操作全部文章、任免管理员、维护硬节点。
 * 超管身份由部署者 SQL 指定，接口只在 0/1 之间变更。
 */
public enum UserRole {
    USER(0),
    ADMIN(1),
    SUPER_ADMIN(2);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 至少是管理员（含超级管理员）——管理后台入口卡口 */
    public boolean isAtLeastAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
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
