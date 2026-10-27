package com.leehonne.examtimeline.library.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 修改记录请求（契约 §7.3）：整体替换式——文字/标签/图片按本次提交全量落。
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateEntryRequest {

    /** 文字内容（可空，但与图片至少一） */
    private String content;

    /** 标签名全量替换（同名复用，空数组=清空标签） */
    private List<String> tagNames;

    /** 图片对象键全量替换（旧键保留、新 tmp 键归档、未提交的删除） */
    private List<String> imageObjectKeys;
}
