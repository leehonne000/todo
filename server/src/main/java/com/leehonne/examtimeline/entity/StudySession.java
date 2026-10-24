package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 学习 session（t_study_session）—— Learn「选科目→横屏计时」的产物（§4.1/§4.2）。
 * 2026-10-24 修订：一次 session 只学一门科目，subject_id 随 session 落库（各科耗时精确统计）。
 * 生命周期：选科目开始计时建行 → 长按结束补 ended_at/duration → Done 写 entry。
 */
@Getter
@Setter
@Entity
@Table(name = "t_study_session")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户（冗余，查询免 join） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 本次学习科目（Learn 入口四选一，一次 session 一门） */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** 计时开始时刻（横屏进入计时那一刻） */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** 长按结束时刻；异常中断可空 */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** 净学习秒数（排除暂停），一切时长统计的原始值 */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
