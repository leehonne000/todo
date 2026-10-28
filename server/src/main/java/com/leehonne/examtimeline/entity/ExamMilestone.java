package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考研硬节点（t_exam_milestone）★ 全局表（无 user_id，人人共享）—— 每年教育部公布后维护。
 * 倒计时 = t_user.exam_year 对应 name='初试' 的 date_from 与今天之差。
 */
@Getter
@Setter
@Entity
@Table(name = "t_exam_milestone")
public class ExamMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 届数：与 t_user.exam_year 对齐 */
    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    /** 节点名：预报名/正式报名/网上确认/准考证下载/初试/出分/国家线/调剂系统开放（8 全国性节点，2026-10-28 定稿） */
    @Column(nullable = false, length = 32)
    private String name;

    /** 节点起始日 */
    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    /** 节点结束日（跨日节点用；单日节点留空） */
    @Column(name = "date_to")
    private LocalDate dateTo;

    /** 时间线展示顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** 数据维护时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
