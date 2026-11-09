package com.leehonne.examtimeline.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 验证码用途（t_verify_code.purpose）：1=注册激活，2=找回密码。防串用。
 * 落库为数字 code（MyBatis-Plus IEnum 自动映射）。
 */
public enum VerifyPurpose implements IEnum<Integer> {
    REGISTER(1),
    RESET_PASSWORD(2);

    private final int code;

    VerifyPurpose(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public static VerifyPurpose fromCode(int code) {
        for (VerifyPurpose p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        throw new IllegalArgumentException("未知 verify purpose: " + code);
    }
}
