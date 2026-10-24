package com.leehonne.examtimeline.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 标签词表（t_tag）—— 用户私有词表，同名复用（user_id+name 唯一），记录删除不影响标签。
 */
@Getter
@Setter
@Entity
@Table(name = "t_tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户：标签是用户私有词表 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 标签文本（chip 上的字） */
    @Column(nullable = false, length = 32)
    private String name;

    /** 首次创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
