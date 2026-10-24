package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.ExamMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamMilestoneRepository extends JpaRepository<ExamMilestone, Long> {

    List<ExamMilestone> findByExamYearOrderBySortOrderAsc(Integer examYear);

    /** 倒计时：届数下 name='初试' 的 date_from */
    Optional<ExamMilestone> findByExamYearAndName(Integer examYear, String name);
}
