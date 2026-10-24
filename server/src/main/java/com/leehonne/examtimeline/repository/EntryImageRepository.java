package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.EntryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryImageRepository extends JpaRepository<EntryImage, Long> {

    List<EntryImage> findByEntryIdOrderBySortOrderAsc(Long entryId);

    long countByEntryId(Long entryId);

    void deleteByEntryId(Long entryId);
}
