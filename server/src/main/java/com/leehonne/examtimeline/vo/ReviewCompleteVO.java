package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 复习完成响应（契约 §6.2）：round=6 出队时 nextDueAt 为 null */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ReviewCompleteVO")
public class ReviewCompleteVO {

    @Schema(description = "round")
    private Integer round;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "nextDueAt")
    private LocalDate nextDueAt;
}
