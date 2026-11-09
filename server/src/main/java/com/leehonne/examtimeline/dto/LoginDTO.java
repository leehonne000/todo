package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 登录：邮箱+密码 */
@Getter
@Setter
@Schema(description = "LoginDTO")
public class LoginDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "email")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "password")
    private String password;
}
