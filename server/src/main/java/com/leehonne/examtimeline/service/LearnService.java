package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.dto.SaveEntriesDTO;
import com.leehonne.examtimeline.vo.StartSessionVO;
import com.leehonne.examtimeline.vo.SubjectVO;

import java.util.List;

/**
 * Learn 流程业务（契约 §5）：选科目 → 计时 → 总结落库。
 * 每条记录入库即自动进艾宾浩斯队列（round=0，next_due_at=记录日+1 天），
 * 记录日 = 学习日（04:00 边界，见 {@link com.leehonne.examtimeline.common.StudyDays}）。
 */
public interface LearnService {

    /** 选科目（§5.1）：注册时定的科目组合，按展示顺序 */
    List<SubjectVO> subjects(Long userId);

    /** 开始计时（§5.2）：进横屏那一刻建行——保证计时中断也不丢统计 */
    StartSessionVO startSession(Long userId, Long subjectId);

    /** 长按结束（§5.3）：写净学习秒数 */
    void finishSession(Long userId, Long sessionId, Integer durationSeconds);

    /** 总结页 Done 批量落库（§5.4）：同事务写 t_entry + 标签关联 + 图片归档；返回新建记录 ID */
    List<Long> saveEntries(Long userId, Long sessionId, List<SaveEntriesDTO.EntryItemDTO> items);
}
