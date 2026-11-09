package com.leehonne.examtimeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自由库列表条目（契约 §7.1，与前端 mock 的 entryListItem 对齐）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "EntryListItemVO")
public class EntryListItemVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "subjectName")
    private String subjectName;

    /** 1=note 2=错题 */
    @Schema(description = "type")
    private Integer type;

    /** 文字内容（纯图片记录为空串） */
    @Schema(description = "content")
    private String content;

    @Schema(description = "tags")
    private List<String> tags;

    /** 图片张数（列表不下发图，详情再取） */
    @Schema(description = "imageCount")
    private Integer imageCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "createdAt")
    private LocalDateTime createdAt;

    /** 已完成轮数 0~6 */
    @Schema(description = "round")
    private Integer round;

    /** 0=队列中 1=已出队 */
    @Schema(description = "reviewStatus")
    private Integer reviewStatus;
}
