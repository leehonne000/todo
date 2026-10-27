package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate date);

    /** 进度模块（§8.1）：区间内签到日期（统计页「签到日历 / 连续签到」） */
    List<CheckIn> findByUserIdAndCheckInDateGreaterThanEqualAndCheckInDateLessThanEqualOrderByCheckInDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
