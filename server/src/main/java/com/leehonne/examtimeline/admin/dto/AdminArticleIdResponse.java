package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 新建文章响应（契约 §11.2） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminArticleIdResponse {

    private Long id;
}
