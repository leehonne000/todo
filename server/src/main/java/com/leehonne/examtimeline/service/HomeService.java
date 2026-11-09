package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.vo.HomeVO;

/**
 * 主界面业务（契约 §4）：状态汇总全现算（产品哲学——无任务实体，不做快照表）。
 */
public interface HomeService {

    /** 一屏状态（§4.1）：签到态 / 今日学习态 / 应复习数 */
    HomeVO home(Long userId);

    /** 每日签到（§4.2）：同日重复 3001（唯一键兜底） */
    void checkIn(Long userId);
}
