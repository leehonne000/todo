package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 找回密码：邮箱+验证码+新密码 */
@Getter
@Setter
@Schema(description = "ResetPasswordDTO")
public class ResetPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "email")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "code")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6~32 位")
    @Schema(description = "newPassword")
    private String newPassword;
}
