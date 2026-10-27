package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 提拔/取消管理员响应（契约 §11.8） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleResponse {

    private Long id;

    private Integer role;
}
