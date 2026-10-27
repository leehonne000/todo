package com.leehonne.examtimeline.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class EntryListItemResponse {

    private Long id;

    private String subjectName;

    /** 1=note 2=错题 */
    private Integer type;

    /** 文字内容（纯图片记录为空串） */
    private String content;

    private List<String> tags;

    /** 图片张数（列表不下发图，详情再取） */
    private Integer imageCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 已完成轮数 0~6 */
    private Integer round;

    /** 0=队列中 1=已出队 */
    private Integer reviewStatus;
}
