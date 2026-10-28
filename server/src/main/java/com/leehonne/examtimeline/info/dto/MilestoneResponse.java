package com.leehonne.examtimeline.info.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class MilestoneResponse {

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    /** 跨日节点的结束日；单日节点为 null */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    private Integer sortOrder;
}
