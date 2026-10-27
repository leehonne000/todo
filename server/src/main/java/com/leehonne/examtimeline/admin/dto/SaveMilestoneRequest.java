package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 新建/编辑硬节点请求（契约 §11.11 增补）——整体替换式。
 * 每年教育部公布后由超级管理员在此维护（产品文档 §5.1"数据化，不写死"）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveMilestoneRequest {

    /** 届数 2000~2100 */
    private Integer examYear;

    /** 节点名（≤32 字）；name='初试' 驱动倒计时，勿随意改名 */
    private String name;

    private LocalDate dateFrom;

    /** 单日节点留空 */
    private LocalDate dateTo;

    /** 时间线展示顺序，缺省 0 */
    private Integer sortOrder;
}
