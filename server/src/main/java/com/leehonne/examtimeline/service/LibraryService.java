package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.dto.UpdateEntryDTO;
import com.leehonne.examtimeline.vo.EntryDetailVO;
import com.leehonne.examtimeline.vo.EntryListItemVO;
import com.leehonne.examtimeline.vo.TagVO;

import java.util.List;

/**
 * 自由库 + 标签业务（契约 §7）——复习模块「总览」本体（产品文档 §5.2）。
 *
 * <p>要点：
 * <ul>
 *   <li>列表/详情只看自己的记录；筛选（科目/类型/标签）后按 createdAt 倒序分页</li>
 *   <li>修改是<strong>整体替换</strong>（文字/标签/图片），但<strong>复习状态不因编辑改变</strong>（§7.3，不重启不顺延）</li>
 *   <li>删除=物理删除，级联清标签关联与图片元数据，MinIO 对象一并清（标签词表独立保留）</li>
 * </ul>
 */
public interface LibraryService {

    /** 列表（§7.1）：自由库浏览与标签查询共用 */
    PageVO<EntryListItemVO> list(Long userId, Long subjectId, Integer type,
                                 String tagIds, Integer page, Integer size);

    /** 详情（§7.2） */
    EntryDetailVO detail(Long userId, Long entryId);

    /** 修改（§7.3）：整体替换式；复习状态不动 */
    void update(Long userId, Long entryId, UpdateEntryDTO req);

    /** 删除（§7.4）：物理删除，级联清关联与图片对象（标签词表独立保留） */
    void delete(Long userId, Long entryId);

    /** 标签词表（§7.5）：按名称升序，q 模糊过滤 */
    List<TagVO> tags(Long userId, String q);
}
