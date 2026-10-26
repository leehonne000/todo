package com.leehonne.examtimeline.learn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 科目（契约 §5.1）：注册时定的科目组合，Learn 入口四选一 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {

    private Long id;

    private String name;

    private Integer sortOrder;
}
