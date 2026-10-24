package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 记录-标签关联（t_entry_tag，多对多中间表，复合主键）。
 */
@Getter
@Setter
@Entity
@Table(name = "t_entry_tag")
@IdClass(EntryTag.EntryTagId.class)
public class EntryTag {

    @Id
    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Id
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    /** 关联时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** 复合主键载体 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryTagId implements Serializable {
        private Long entryId;
        private Long tagId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntryTagId other)) return false;
            return Objects.equals(entryId, other.entryId) && Objects.equals(tagId, other.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entryId, tagId);
        }
    }
}
