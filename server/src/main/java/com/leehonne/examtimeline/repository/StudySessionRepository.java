package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /** 进度模块：按日期范围聚合各科目时长（配合 idx_session_user_started） */
    List<StudySession> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            Long userId, LocalDateTime from, LocalDateTime to);

    /** 今日学习卡打勾：当天有无 session */
    boolean existsByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            Long userId, LocalDateTime from, LocalDateTime to);
}
