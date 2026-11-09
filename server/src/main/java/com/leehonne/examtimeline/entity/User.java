package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leehonne.examtimeline.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户（t_user）—— 注册制多用户；登录=邮箱+密码；届数驱动倒计时（产品文档 §1.2/§5.1）。
 * MyBatis-Plus Entity（充当 PO），主键跟随数据库 AUTO_INCREMENT 自增。
 */
@Getter
@Setter
@TableName("t_user")
@Schema(description = "用户实体")
public class User {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 登录标识：注册/登录/找回密码都用它 */
    @Schema(description = "登录邮箱")
    private String email;

    /** BCrypt 密码哈希，不存明文 */
    @Schema(description = "BCrypt 密码哈希")
    private String passwordHash;

    /** 邮箱激活标志：false=未激活(禁登录) true=已激活 */
    @Schema(description = "邮箱是否已激活")
    private Boolean emailVerified = false;

    /** 角色：0=普通用户 1=管理员 2=超级管理员（2026-10-27 三级制） */
    @Schema(description = "角色：0=普通用户 1=管理员 2=超级管理员")
    private UserRole role = UserRole.USER;

    /** 手机号（预留，v1 不做短信验证） */
    @Schema(description = "手机号（预留）")
    private String phone;

    /** 考研届数（如 2027）：倒计时/时间线取数据的依据 */
    @Schema(description = "考研届数")
    private Integer examYear;

    /** 昵称（1~20 字符）；注册自动赋默认 = 邮箱 @ 前缀（§3.5） */
    @Schema(description = "昵称")
    private String nickname;

    /** 头像对象键；空 = 系统默认头像 /avatar-default.png（§3.5） */
    @Schema(description = "头像对象键")
    private String avatarObjectKey;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
