package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByUserIdOrderBySortOrderAsc(Long userId);

    Optional<Subject> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
