package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "AdminMilestoneVO")
public class AdminMilestoneVO {

    @Schema(description = "id")
    private Long id;

    /** 届数（如 2027），与 t_user.exam_year 对齐 */
    @Schema(description = "examYear")
    private Integer examYear;

    /** 节点名：预报名/正式报名/网上确认/准考证下载/初试/出分/国家线/调剂系统开放（8 全国性节点，2026-10-28 定稿） */
    @Schema(description = "name")
    private String name;

    @Schema(description = "dateFrom")
    private LocalDate dateFrom;

    /** 单日节点为 null */
    @Schema(description = "dateTo")
    private LocalDate dateTo;

    @Schema(description = "sortOrder")
    private Integer sortOrder;
}
