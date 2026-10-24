package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.EntryTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryTagRepository extends JpaRepository<EntryTag, EntryTag.EntryTagId> {

    List<EntryTag> findByEntryId(Long entryId);

    List<EntryTag> findByTagId(Long tagId);

    void deleteByEntryId(Long entryId);
}
