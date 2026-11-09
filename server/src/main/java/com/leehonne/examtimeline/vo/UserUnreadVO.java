package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 用户未读回复（契约 §9.6）：信息入口红点 + 有未读的文章定位 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "UserUnreadVO")
public class UserUnreadVO {

    /** 未读回复总数（红点判定：>0 显示） */
    @Schema(description = "count")
    private long count;

    /** 有未读回复的文章 id 列表（信息页文章卡红点定位用） */
    @Schema(description = "articleIds")
    private List<Long> articleIds;
}
