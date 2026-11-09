package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 某科目累计秒数（契约 §8.1 bySubject[]） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SubjectSecondsVO")
public class SubjectSecondsVO {

    @Schema(description = "subjectName")
    private String subjectName;

    @Schema(description = "seconds")
    private int seconds;
}
