package com.leehonne.examtimeline.enums;

/**
 * 文章状态（t_article.status）：0=草稿（仅管理员可见），1=已发布（全员可见）。下线=改回草稿。
 */
public enum ArticleStatus {
    DRAFT(0),
    PUBLISHED(1);

    private final int code;

    ArticleStatus(int code) {
        this.code = code;
    }

    public int getCode() {
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
