package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 硬节点管理列表（契约 §11.10 增补）：examYear 升序、sortOrder 升序 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMilestoneListResponse {

    private List<AdminMilestoneResponse> items;
}
