package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考研硬节点（t_exam_milestone）—— 按届数维护的倒计时/时间线节点（§9）。
 */
@Getter
@Setter
@TableName("t_exam_milestone")
@Schema(description = "考研硬节点实体")
public class ExamMilestone {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "考研届数")
    private Integer examYear;

    @Schema(description = "节点名")
    private String name;

    @Schema(description = "开始日期")
    private LocalDate dateFrom;

    @Schema(description = "结束日期（可空=单日节点）")
    private LocalDate dateTo;

    @Schema(description = "排序序号")
    private Integer sortOrder = 0;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
