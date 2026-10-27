package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 硬节点管理条目（契约 §11.10 增补）——比 §9.1 的 C 端响应多 id（编辑/删除入口）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMilestoneResponse {

    private Long id;

    /** 届数（如 2027），与 t_user.exam_year 对齐 */
    private Integer examYear;

    /** 节点名：预报名/正式报名/网上确认/准考证下载/初试/出分/国家线/复试调剂 */
    private String name;

    private LocalDate dateFrom;

    /** 单日节点为 null */
    private LocalDate dateTo;

    private Integer sortOrder;
}
