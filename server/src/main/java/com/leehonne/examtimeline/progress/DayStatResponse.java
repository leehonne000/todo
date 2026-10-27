package com.leehonne.examtimeline.progress;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** 单日统计（契约 §8.1 days[]），按学习日 04:00 口径归桶 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayStatResponse {

    /** 学习日（04:00 边界归桶后的「天」） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** 当日净学习秒数（session.duration_seconds 求和） */
    private int totalSeconds;

    /** 当日各科目耗时（秒，降序） */
    private List<SubjectSecondsResponse> bySubject;

    /** 当日 note 条数 */
    private int noteCount;

    /** 当日错题条数 */
    private int wrongCount;
}
