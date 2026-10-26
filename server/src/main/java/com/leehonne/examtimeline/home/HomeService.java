package com.leehonne.examtimeline.home;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.CheckIn;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.repository.CheckInRepository;
import com.leehonne.examtimeline.repository.EntryRepository;
import com.leehonne.examtimeline.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 主界面业务（契约 §4）：状态汇总全现算（产品哲学——无任务实体，不做快照表）。
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final CheckInRepository checkInRepository;
    private final StudySessionRepository sessionRepository;
    private final EntryRepository entryRepository;

    /** 一屏状态（§4.1）：签到态 / 今日学习态 / 应复习数 */
    public HomeResponse home(Long userId) {
        LocalDate today = StudyDays.today();
        boolean checkedInToday = checkInRepository.findByUserIdAndCheckInDate(userId, today).isPresent();
        // 今日学习 = 学习日窗口 [今天04:00, 明天04:00) 内有 session
        boolean todayLearned = sessionRepository.existsByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                userId, StudyDays.dayStart(today), StudyDays.dayStart(today.plusDays(1)));
        long dueReviewCount = entryRepository.countByUserIdAndReviewStatusAndNextDueAtLessThanEqual(
                userId, ReviewStatus.QUEUED, today);
        return new HomeResponse(checkedInToday, todayLearned, dueReviewCount);
    }

    /** 每日签到（§4.2）：同日重复 3001（唯一键兜底） */
    @Transactional
    public void checkIn(Long userId) {
        LocalDate today = StudyDays.today();
        if (checkInRepository.findByUserIdAndCheckInDate(userId, today).isPresent()) {
            throw BizException.conflict("今天已签到");
        }
        CheckIn c = new CheckIn();
        c.setUserId(userId);
        c.setCheckInDate(today);
        try {
            checkInRepository.saveAndFlush(c);
        } catch (DataIntegrityViolationException e) {
            // 并发双击兜底：唯一键 (user_id, check_in_date)
            throw BizException.conflict("今天已签到");
        }
    }
}
