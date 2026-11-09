package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 主界面聚合（契约 §4.1）：一屏三块状态，全按学习日（04:00 口径）现算。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "HomeVO")
public class HomeVO {

    /** 今天是否已签到 */
    @Schema(description = "checkedInToday")
    private boolean checkedInToday;

    /** 今天有无学习 session（今日学习卡 ✓） */
    @Schema(description = "todayLearned")
    private boolean todayLearned;

    /** 今日应复习条数（Review 卡数字） */
    @Schema(description = "dueReviewCount")
    private long dueReviewCount;
}
