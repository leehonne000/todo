package com.leehonne.examtimeline.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户资料（契约 §3.5）：自定义页头像+昵称。
 * GET 返回全量；PUT 也回全量（前端可整体替换本地缓存）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long userId;

    private String email;

    private String nickname;

    /** 角色：0=普通用户 1=超级管理员（2026-10-27 增补，管理后台入口门禁用） */
    private Integer role;

    /** 可直接渲染的地址：默认 /avatar-default.png；对象键则为预签名 URL */
    private String avatarUrl;

    private Integer examYear;
}
