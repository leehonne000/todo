package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 管理端未读留言数（契约 §11.16）：「留言管理」侧栏红点 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AdminUnreadVO")
public class AdminUnreadVO {

    /** 未读留言条数（用户发来、管理员未读；超管=全局口径） */
    @Schema(description = "count")
    private long count;
}
