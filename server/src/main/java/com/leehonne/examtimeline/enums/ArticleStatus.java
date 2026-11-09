package com.leehonne.examtimeline.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 文章状态（t_article.status）：0=草稿（仅管理员可见），1=已发布（全员可见）。下线=改回草稿。
 * 落库为数字 code（MyBatis-Plus IEnum 自动映射）。
 */
public enum ArticleStatus implements IEnum<Integer> {
    DRAFT(0),
    PUBLISHED(1);

    private final int code;

    ArticleStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public static ArticleStatus fromCode(int code) {
        for (ArticleStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知 article status: " + code);
    }
}
