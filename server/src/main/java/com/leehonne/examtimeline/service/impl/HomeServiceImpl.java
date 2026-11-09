package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.CheckIn;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.StudySession;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.CheckInMapper;
import com.leehonne.examtimeline.mapper.EntryMapper;
import com.leehonne.examtimeline.mapper.StudySessionMapper;
import com.leehonne.examtimeline.service.HomeService;
import com.leehonne.examtimeline.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 主界面业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper）。
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final CheckInMapper checkInMapper;
    private final StudySessionMapper sessionMapper;
    private final EntryMapper entryMapper;

    @Override
    public HomeVO home(Long userId) {
        LocalDate today = StudyDays.today();
        boolean checkedInToday = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getCheckInDate, today)) > 0;
        // 今日学习 = 学习日窗口 [今天04:00, 明天04:00) 内有 session
        boolean todayLearned = sessionMapper.selectCount(new LambdaQueryWrapper<StudySession>()
                .eq(StudySession::getUserId, userId)
                .ge(StudySession::getStartedAt, StudyDays.dayStart(today))
                .lt(StudySession::getStartedAt, StudyDays.dayStart(today.plusDays(1)))) > 0;
        long dueReviewCount = entryMapper.selectCount(new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, userId)
                .eq(Entry::getReviewStatus, ReviewStatus.QUEUED)
                .le(Entry::getNextDueAt, today));
        return new HomeVO(checkedInToday, todayLearned, dueReviewCount);
    }

    @Override
    @Transactional
    public void checkIn(Long userId) {
        LocalDate today = StudyDays.today();
        if (checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getCheckInDate, today)) > 0) {
            throw BusinessException.conflict("今天已签到");
        }
        CheckIn c = new CheckIn();
        c.setUserId(userId);
        c.setCheckInDate(today);
        try {
            checkInMapper.insert(c);
        } catch (DataIntegrityViolationException e) {
            // 并发双击兜底：唯一键 (user_id, check_in_date)
            throw BusinessException.conflict("今天已签到");
        }
    }
}
