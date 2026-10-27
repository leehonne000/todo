package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    /** Review 卡今日到期（idx_entry_review） */
    List<Entry> findByUserIdAndReviewStatusAndNextDueAtLessThanEqual(
            Long userId, ReviewStatus reviewStatus, LocalDate day);

    /** 主界面 Review 卡数字：今日应复习条数 */
    long countByUserIdAndReviewStatusAndNextDueAtLessThanEqual(
            Long userId, ReviewStatus reviewStatus, LocalDate day);

    /** 自由库浏览/筛选 */
    List<Entry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Entry> findByUserIdAndSubjectIdOrderByCreatedAtDesc(Long userId, Long subjectId);

    /** 某次学习的全部记录 */
    List<Entry> findByUserIdAndSessionId(Long userId, Long sessionId);

    /** 进度模块（§8.1）：区间内入库的记录，按 created_at 的学习日归桶数 note/错题 */
    List<Entry> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId, LocalDateTime from, LocalDateTime to);
}
