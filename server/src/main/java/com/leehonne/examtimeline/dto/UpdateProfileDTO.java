package com.leehonne.examtimeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 修改资料请求（契约 §3.5.2）：整体可选，至少传一项 */
@Getter
@Setter
@Schema(description = "UpdateProfileDTO")
public class UpdateProfileDTO {

    /** 昵称 1~20 字符 */
    @Schema(description = "nickname")
    private String nickname;

    /** 头像对象键（先调 §5.5 上传拿 objectKey） */
    @Schema(description = "avatarObjectKey")
    private String avatarObjectKey;
}
