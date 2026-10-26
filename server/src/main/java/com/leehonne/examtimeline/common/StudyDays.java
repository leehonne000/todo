package com.leehonne.examtimeline.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习日口径 + 艾宾浩斯六轮节奏（契约 §1.1 / §6.2）。
 *
 * <p>「一天」= 04:00–次日 04:00，记在<strong>起始日</strong>（凌晨 4 点前的时刻归前一天），
 * 适配考研夜战作息。统计归桶、今日学习/签到、到期判断、倒计时全站统一走这里，
 * 不要各自造轮子。后端 SQL 侧对应 {@code DATE(时间戳 - INTERVAL 4 HOUR)}。
 *
 * <p>艾宾浩斯节奏（相对<strong>记录日</strong>的天数）：入库 +1 天首次到期，
 * 每完成一轮复习按 2/4/7/15/22 顺延，六轮走完出队。
 */
public final class StudyDays {

    /** 六轮间隔（相对记录日的天数）；round=r（0~5）时 next_due_at = 记录日 + INTERVALS[r] */
    public static final int[] INTERVALS = {1, 2, 4, 7, 15, 22};

    private StudyDays() {
    }

    /** 时刻 → 学习日（04:00 边界） */
    public static LocalDate studyDate(LocalDateTime ts) {
        return ts.minusHours(4).toLocalDate();
    }

    /** 当前学习日 */
    public static LocalDate today() {
        return studyDate(LocalDateTime.now());
    }

    /**
     * 记录日 → 第 round 轮到期日。
     * 入库用 round=0（记录日+1 天）；复习完成推进 round 后顺延。
     */
    public static LocalDate dueDate(LocalDate recordDate, int round) {
        return recordDate.plusDays(INTERVALS[round]);
    }
}
