package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 管理文章分页列表（契约 §11.1，含草稿） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminArticleListResponse {

    private List<AdminArticleListItemResponse> items;

    private int total;

    private int page;

    private int size;
}
