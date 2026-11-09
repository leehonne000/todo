package com.leehonne.examtimeline.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 记录类型（t_entry.type）：1=note，2=错题。
 * 语义见 docs/数据库设计.md §2.4——总结页「添加note / 添加错题」入口按钮的落库值。
 * 落库为数字 code（MyBatis-Plus IEnum 自动映射）。
 */
public enum EntryType implements IEnum<Integer> {
    NOTE(1),
    WRONG(2);

    private final int code;

    EntryType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public static EntryType fromCode(int code) {
        for (EntryType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知 entry type: " + code);
    }
}
