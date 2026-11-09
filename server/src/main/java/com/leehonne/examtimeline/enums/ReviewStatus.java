package com.leehonne.examtimeline.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 复习状态（t_entry.review_status）：0=队列中，1=已出队（六轮走完）。
 * 落库为数字 code（MyBatis-Plus IEnum 自动映射）。
 */
public enum ReviewStatus implements IEnum<Integer> {
    QUEUED(0),
    DONE(1);

    private final int code;

    ReviewStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public static ReviewStatus fromCode(int code) {
        for (ReviewStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 review_status: " + code);
    }
}
