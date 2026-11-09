package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.vo.ReviewCompleteVO;
import com.leehonne.examtimeline.vo.ReviewItemVO;

import java.util.List;

/**
 * Review 复习队列业务（契约 §6）——艾宾浩斯队列唯一消费入口。
 * 到期口径：review_status=0 AND next_due_at <= 今天（学习日）；
 * 复习完成 round+1 按节奏顺延，六轮走完出队（无自判/无重启）。
 */
public interface ReviewService {

    /** 今日到期列表（§6.1）：跨科目堆叠，越早欠的排越前 */
    List<ReviewItemVO> due(Long userId);

    /** 复习完成（§6.2）：无自判——完成该项即推进一轮；round=6 出队 */
    ReviewCompleteVO complete(Long userId, Long entryId);
}
