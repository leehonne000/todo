package com.leehonne.examtimeline.enums;

/**
 * 验证码用途（t_verify_code.purpose）：1=注册激活，2=找回密码。防串用。
 */
public enum VerifyPurpose {
    REGISTER(1),
    RESET_PASSWORD(2);

    private final int code;

    VerifyPurpose(int code) {
        this.code = code;
    }

    public int getCode() {
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
