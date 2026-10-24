package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户（t_user）—— 注册制多用户；登录=邮箱+密码；届数驱动倒计时（产品文档 §1.2/§5.1）。
 */
@Getter
@Setter
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录标识：注册/登录/找回密码都用它 */
    @Column(nullable = false, unique = true, length = 64)
    private String email;

    /** BCrypt 密码哈希，不存明文 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** 邮箱激活标志：0=未激活(禁登录) 1=已激活 */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /** 手机号（预留，v1 不做短信验证） */
    @Column(unique = true, length = 20)
    private String phone;

    /** 考研届数（如 2027）：倒计时/时间线取数据的依据 */
    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
