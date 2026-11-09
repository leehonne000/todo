package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 结束计时请求（契约 §5.3）：净学习秒数（排除暂停，暂停是前端本地逻辑） */
@Getter
@Setter
@Schema(description = "FinishSessionDTO")
public class FinishSessionDTO {

    @NotNull(message = "durationSeconds 不能为空")
    @Min(value = 0, message = "durationSeconds 不能为负数")
    @Schema(description = "durationSeconds")
    private Integer durationSeconds;
}
