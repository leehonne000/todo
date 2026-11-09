package com.leehonne.examtimeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 记录-标签关联（t_entry_tag，多对多中间表，复合主键 entry_id + tag_id）。
 * 复合主键表不声明 @TableId，统一用 Wrapper 条件操作（selectList / delete / insert）。
 */
@Getter
@Setter
@TableName("t_entry_tag")
@Schema(description = "记录-标签关联实体")
public class EntryTag {

    @Schema(description = "记录项ID（复合主键）")
    private Long entryId;

    @Schema(description = "标签ID（复合主键）")
    private Long tagId;

    /** 关联时间 */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "关联时间")
    private LocalDateTime createdAt;
}
