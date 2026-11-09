package com.leehonne.examtimeline.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 发送/回复留言请求体（§9.4/§11.15）：content 1~300 字 */
@Getter
@Setter
@Schema(description = "SendMessageDTO")
public class SendMessageDTO {

    @Schema(description = "content")
    private String content;
}
