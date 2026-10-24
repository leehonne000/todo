package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到（t_check_in）—— (user_id, check_in_date) 唯一，天然防重复打卡。
 */
@Getter
@Setter
@Entity
@Table(name = "t_check_in")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 打卡自然日；UNIQUE 天然防重复打卡 */
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
