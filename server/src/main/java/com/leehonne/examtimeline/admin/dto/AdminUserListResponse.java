package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 管理用户分页列表（契约 §11.7） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {

    private List<AdminUserListItemResponse> items;

    private int total;

    private int page;

    private int size;
}
