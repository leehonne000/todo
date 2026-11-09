package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 开始计时请求（契约 §5.2）：一次 session 只学一门 */
@Getter
@Setter
@Schema(description = "StartSessionDTO")
public class StartSessionDTO {

    @NotNull(message = "subjectId 不能为空")
    @Schema(description = "subjectId")
    private Long subjectId;
}
