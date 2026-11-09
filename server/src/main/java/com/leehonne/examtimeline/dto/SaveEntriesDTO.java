package com.leehonne.examtimeline.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 总结页 Done 批量落库请求（契约 §5.4）：本次全部子条目，一条 = 一条 t_entry */
@Getter
@Setter
@Schema(description = "SaveEntriesDTO")
public class SaveEntriesDTO {

    @NotNull(message = "entries 必须是数组")
    @Schema(description = "entries")
    private List<EntryItemDTO> entries;

    /** 单条记录（总结页子条目 + 记录编辑内容） */
    @Getter
    @Setter
    public static class EntryItemDTO {

        /** 1=note 2=错题（总结页入口按钮定死，§4.3） */
        @NotNull(message = "type 不能为空")
        @Schema(description = "type")
        private Integer type;

        /** 文字可空（纯图片记录）；与图片至少有一 */
        @Schema(description = "content")
        private String content;

        /** 标签名（同名复用，自动建词表） */
        @Schema(description = "tagNames")
        private List<String> tagNames;

        /** 上传接口返回的 objectKey（临时键，落库时归档）；每条最多 10 张 */
        @Schema(description = "imageObjectKeys")
        private List<String> imageObjectKeys;
    }
}
