package com.leehonne.examtimeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 提拔/取消超级管理员请求（契约 §11.8） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UpdateRoleDTO")
public class UpdateRoleDTO {

    /** 1=提拔为超级管理员，0=取消 */
    @Schema(description = "role")
    private Integer role;
}
