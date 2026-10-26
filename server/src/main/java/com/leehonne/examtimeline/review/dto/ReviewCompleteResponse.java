package com.leehonne.examtimeline.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ReviewCompleteResponse {

    private Integer round;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueAt;
}
