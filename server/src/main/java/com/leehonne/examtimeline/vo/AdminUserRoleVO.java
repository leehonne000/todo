package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 提拔/取消管理员响应（契约 §11.8） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AdminUserRoleVO")
public class AdminUserRoleVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "role")
    private Integer role;
}
