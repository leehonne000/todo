package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 注册：邮箱+密码+届数+科目组合+激活验证码（校验通过即 email_verified=1） */
@Getter
@Setter
@Schema(description = "RegisterDTO")
public class RegisterDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "email")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6~32 位")
    @Schema(description = "password")
    private String password;

    @NotNull(message = "考研届数不能为空")
    @Schema(description = "examYear")
    private Integer examYear;

    /** 科目组合（2~5 门，如 政治/英语一/数学三/408） */
    @NotNull(message = "科目组合不能为空")
    @Size(min = 2, max = 5, message = "科目须为 2~5 门")
    @Schema(description = "subjects")
    private List<String> subjects;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "code")
    private String code;
}
