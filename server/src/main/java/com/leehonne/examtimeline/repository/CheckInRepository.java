package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate date);
}
