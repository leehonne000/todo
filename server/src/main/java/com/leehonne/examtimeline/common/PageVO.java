package com.leehonne.examtimeline.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页 VO（课程第 11 课 PageVO 设计）—— 统一列表分页出参外壳。
 */
@Data
@Schema(description = "分页结果")
public class PageVO<T> implements Serializable {

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页大小")
    private Long size;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "当前页数据列表")
    private List<T> records;

    public static <T> PageVO<T> of(long current, long size, long total, List<T> records) {
        PageVO<T> page = new PageVO<>();
        page.setCurrent(current);
        page.setSize(size);
        page.setTotal(total);
        page.setPages(size == 0 ? 0 : (total + size - 1) / size);
        page.setRecords(records);
        return page;
    }
}
