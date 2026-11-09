package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到（t_check_in）—— (user_id, check_in_date) 唯一，天然防重复打卡。
 */
@Getter
@Setter
@TableName("t_check_in")
@Schema(description = "每日签到实体")
public class CheckIn {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    /** 打卡自然日；UNIQUE 天然防重复打卡 */
    @Schema(description = "打卡日期")
    private LocalDate checkInDate;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
