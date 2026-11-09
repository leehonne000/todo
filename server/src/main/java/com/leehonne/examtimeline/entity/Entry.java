package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@TableName("t_entry")
@Schema(description = "记录项实体")
public class Entry {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 来自哪次 Learn：把"学了多久"和"学了什么"钉在一起（§8） */
    @Schema(description = "所属学习会话ID")
    private Long sessionId;

    /** 归属用户（冗余，查询免 join） */
    @Schema(description = "用户ID")
    private Long userId;

    /** 所属科目：继承自 session.subject_id（冗余免 join） */
    @Schema(description = "科目ID")
    private Long subjectId;

    /** 类型：1=note 2=错题（总结页「添加note/添加错题」入口按钮，§4.3） */
    @Schema(description = "类型：1=note 2=错题")
    private EntryType type;

    /** 文字内容，可空（纯图片记录）；应用层校验"文字与图片至少一" */
    @Schema(description = "文字内容")
    private String content;

    /** 艾宾浩斯已完成轮数 0~6 */
    @Schema(description = "已完成复习轮数 0~6")
    private Integer round = 0;

    /** 下一轮到期日：入库=记录日+1天，此后按 2/4/7/15/22 顺延（§6.1）；出队后为空 */
    @Schema(description = "下一轮到期日")
    private LocalDate nextDueAt;

    /** 复习状态：0=队列中 1=已出队（六轮走完） */
    @Schema(description = "复习状态：0=队列中 1=已出队")
    private ReviewStatus reviewStatus = ReviewStatus.QUEUED;

    /** 入库时间（Done 时刻）= 复习排期基准日 */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "入库时间")
    private LocalDateTime createdAt;

    /** 最后编辑时间（自由库可改内容） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后编辑时间")
    private LocalDateTime updatedAt;
}
