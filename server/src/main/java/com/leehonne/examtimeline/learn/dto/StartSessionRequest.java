package com.leehonne.examtimeline.learn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 开始计时请求（契约 §5.2）：一次 session 只学一门 */
@Getter
@Setter
public class StartSessionRequest {

    @NotNull(message = "subjectId 不能为空")
    private Long subjectId;
}
