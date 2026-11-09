package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 发验证码：purpose 1=注册激活 2=找回密码 */
@Getter
@Setter
@Schema(description = "SendCodeDTO")
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "email")
    private String email;

    /** 1=注册激活，2=找回密码 */
    @NotNull(message = "验证码用途不能为空")
    @Schema(description = "purpose")
    private Integer purpose;
}
