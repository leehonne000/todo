package com.leehonne.examtimeline.entity;

import com.leehonne.examtimeline.enums.VerifyPurpose;
import com.leehonne.examtimeline.enums.VerifyPurposeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 邮箱验证码（t_verify_code）—— 注册激活 + 找回密码共用（purpose 防串用）；v1 落 MySQL 不引 Redis。
 * 校验：取最新未过期未使用的码比对，通过置 used_at（防重复使用）；发码 60s 限频。
 */
@Getter
@Setter
@Entity
@Table(name = "t_verify_code")
public class VerifyCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 收码邮箱（无外键：找回密码时用户未登录） */
    @Column(nullable = false, length = 64)
    private String email;

    /** 6 位数字验证码 */
    @Column(nullable = false, length = 8)
    private String code;

    /** 用途：1=注册激活 2=找回密码（防串用） */
    @Convert(converter = VerifyPurposeConverter.class)
    @Column(nullable = false)
    private VerifyPurpose purpose;

    /** 过期时刻（发码+10 分钟） */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 消费时刻；NULL=未用，防重复使用 */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** 发码时间（同邮箱 60s 限频依据） */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
