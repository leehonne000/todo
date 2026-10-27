package com.leehonne.examtimeline.library.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 标签词表条目（契约 §7.5） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {

    private Long id;

    private String name;
}
