package com.leehonne.examtimeline.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 标签词表条目（契约 §7.5） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TagVO")
public class TagVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "name")
    private String name;
}
