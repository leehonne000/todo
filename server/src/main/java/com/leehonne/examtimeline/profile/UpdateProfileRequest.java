package com.leehonne.examtimeline.profile;

import lombok.Getter;
import lombok.Setter;

/** 修改资料请求（契约 §3.5.2）：整体可选，至少传一项 */
@Getter
@Setter
public class UpdateProfileRequest {

    /** 昵称 1~20 字符 */
    private String nickname;

    /** 头像对象键（先调 §5.5 上传拿 objectKey） */
    private String avatarObjectKey;
}
