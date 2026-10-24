package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByUserIdAndName(Long userId, String name);

    List<Tag> findByUserIdOrderByNameAsc(Long userId);
}
