package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 考研硬节点（契约 §9.1 milestones[]） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MilestoneVO")
public class MilestoneVO {

    @Schema(description = "name")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "dateFrom")
    private LocalDate dateFrom;

    /** 跨日节点的结束日；单日节点为 null */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "dateTo")
    private LocalDate dateTo;

    @Schema(description = "sortOrder")
    private Integer sortOrder;
}
