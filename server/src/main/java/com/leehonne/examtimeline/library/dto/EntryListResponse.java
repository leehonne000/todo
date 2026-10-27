package com.leehonne.examtimeline.library.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 自由库分页列表（契约 §7.1） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntryListResponse {

    private List<EntryListItemResponse> items;

    /** 筛选后的总条数 */
    private int total;

    private int page;

    private int size;
}
