package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.vo.ProgressVO;

/**
 * 进度模块业务（契约 §5.3 / §8.1）：统计页「学习进度总结」的数据引擎。
 *
 * <p>口径（数据库设计 §4，全部精确）：
 * <ul>
 *   <li>时长：session 净学习秒数按 started_at 的学习日 + 科目汇总（一次 session 一门）</li>
 *   <li>条数：entry 按 created_at 的学习日 + type（1=note / 2=错题）计数</li>
 *   <li>签到：t_check_in.check_in_date 落在区间内即计入</li>
 *   <li>「天」= 04:00–次日 04:00，记在起始日（StudyDays）</li>
 * </ul>
 */
public interface ProgressService {

    /** 统计（§8.1）：from/to 缺省 = 本月 1 日 ~ 今天（学习日） */
    ProgressVO progress(Long userId, String fromStr, String toStr);
}
