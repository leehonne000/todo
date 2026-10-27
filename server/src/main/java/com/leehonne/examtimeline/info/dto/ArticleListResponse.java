package com.leehonne.examtimeline.info.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 博客分页列表（契约 §9.2，只含已发布） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleListResponse {

    private List<ArticleListItemResponse> items;

    private int total;

    private int page;

    private int size;
}
