package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 提拔/取消超级管理员请求（契约 §11.8） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    /** 1=提拔为超级管理员，0=取消 */
    private Integer role;
}
