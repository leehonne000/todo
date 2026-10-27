package com.leehonne.examtimeline.progress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 某科目累计秒数（契约 §8.1 bySubject[]） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSecondsResponse {

    private String subjectName;

    private int seconds;
}
