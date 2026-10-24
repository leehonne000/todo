package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 科目组合（t_subject）—— 注册时确定（2~5 门）；Learn 入口选科目四选一（§4.1）；自由库/统计维度。
 * 删除科目不级联删记录（t_entry.subject_id 外键 RESTRICT，防误删历史）。
 */
@Getter
@Setter
@Entity
@Table(name = "t_subject")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户（冗余，查询免 join） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 科目名，用户自定义（政治/英语一/数学三/408…）；同用户不重名 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 展示顺序：选科目列表与统计表头顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
