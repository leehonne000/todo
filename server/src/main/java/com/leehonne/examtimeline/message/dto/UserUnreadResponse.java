package com.leehonne.examtimeline.message.dto;

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
public class UserUnreadResponse {

    /** 未读回复总数（红点判定：>0 显示） */
    private long count;

    /** 有未读回复的文章 id 列表（信息页文章卡红点定位用） */
    private List<Long> articleIds;
}
