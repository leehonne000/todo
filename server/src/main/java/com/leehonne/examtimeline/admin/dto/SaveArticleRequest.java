package com.leehonne.examtimeline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 新建/编辑文章请求（契约 §11.2/§11.3）——整体替换式：
 * coverObjectKey 缺省/空 = 无封面（编辑时会清掉旧封面）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveArticleRequest {

    /** 标题（≤120 字） */
    private String title;

    /** 正文（v1 纯文本/Markdown 原文） */
    private String content;

    /** 封面对象键：§5.5 上传的临时键（user_{uid}/tmp/...）；编辑时也可原样回传旧键表示不变 */
    private String coverObjectKey;
}
