package com.leehonne.examtimeline.message.dto;

import lombok.Getter;
import lombok.Setter;

/** 发送/回复留言请求体（§9.4/§11.15）：content 1~300 字 */
@Getter
@Setter
public class SendMessageRequest {

    private String content;
}
