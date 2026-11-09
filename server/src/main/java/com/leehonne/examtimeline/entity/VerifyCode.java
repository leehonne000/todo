package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leehonne.examtimeline.enums.VerifyPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 验证码（t_verify_code）—— 10 分钟有效、只用一次、同邮箱同用途 60s 限频（docs/数据库设计.md §2.9）。
 */
@Getter
@Setter
@TableName("t_verify_code")
@Schema(description = "验证码实体")
public class VerifyCode {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "6 位验证码")
    private String code;

    /** 用途：1=注册激活 2=找回密码（防串用） */
    @Schema(description = "用途：1=注册激活 2=找回密码")
    private VerifyPurpose purpose;

    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    /** 使用时间；空=未使用 */
    @Schema(description = "使用时间")
    private LocalDateTime usedAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
