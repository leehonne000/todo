package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 进度模块统计（契约 §8.1）：以日期为轴的每日统计 + 区间内签到日期。
 * days 为稠密数组——[from, to] 每天一项（无数据的天各项为 0/空），与前端 mock 语义一致。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ProgressVO")
public class ProgressVO {

    @Schema(description = "days")
    private List<DayStatVO> days;

    /** 区间内签到日期（yyyy-MM-dd 升序），统计页「签到日历 / 连续签到」用 */
    @Schema(description = "checkInDates")
    private List<String> checkInDates;
}
