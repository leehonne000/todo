package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 信息模块固定区（契约 §9.1）：倒计时 + 硬节点时间线。
 * daysLeft = 用户届数下「初试」date_from − 学习日今天（可为负：已过初试）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "InfoOverviewVO")
public class InfoOverviewVO {

    /** 用户考研届数（t_user.exam_year） */
    @Schema(description = "examYear")
    private Integer examYear;

    /** 距初试天数；缺「初试」节点数据时为 null */
    @Schema(description = "daysLeft")
    private Integer daysLeft;

    /** 硬节点时间线（sortOrder 升序） */
    @Schema(description = "milestones")
    private List<MilestoneVO> milestones;
}
