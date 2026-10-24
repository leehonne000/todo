package com.leehonne.examtimeline.entity;

import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.enums.EntryTypeConverter;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.enums.ReviewStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记录项（t_entry）★ 核心表 —— note/错题本体 + 内嵌艾宾浩斯复习状态机。
 * 状态机：round 0~6；入库 round=0, next_due_at=记录日+1；复习完成 round+1 顺延；
 * round=6 → review_status=1, next_due_at=NULL 出队（§6）。
 */
@Getter
@Setter
@Entity
@Table(name = "t_entry")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来自哪次 Learn：把"学了多久"和"学了什么"钉在一起（§8） */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 归属用户（冗余，查询免 join） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 所属科目：继承自 session.subject_id（冗余免 join） */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** 类型：1=note 2=错题（总结页「添加note/添加错题」入口按钮，§4.3） */
    @Convert(converter = EntryTypeConverter.class)
    @Column(nullable = false)
    private EntryType type;

    /** 文字内容，可空（纯图片记录）；应用层校验"文字与图片至少一" */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 艾宾浩斯已完成轮数 0~6 */
    @Column(nullable = false)
    private Integer round = 0;

    /** 下一轮到期日：入库=记录日+1天，此后按 2/4/7/15/22 顺延（§6.1） */
    @Column(name = "next_due_at")
    private LocalDate nextDueAt;

    /** 复习状态：0=队列中 1=已出队（六轮走完） */
    @Convert(converter = ReviewStatusConverter.class)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.QUEUED;

    /** 入库时间（Done 时刻）= 复习排期基准日 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 最后编辑时间（自由库可改内容） */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
