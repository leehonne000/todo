package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

import com.leehonne.examtimeline.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自由库接口（契约 §7.1–§7.4）：浏览/筛选/详情/修改/删除。
 */
@RestController
@RequiredArgsConstructor
public class EntryController {

    private final LibraryService libraryService;

    /** 列表（§7.1）：自由库浏览 + 标签查询共用；tagIds 逗号分隔（任一命中） */
    @GetMapping("/api/entries")
    public Result<PageVO<EntryListItemVO>> list(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "tagIds", required = false) String tagIds,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.ok(libraryService.list(CurrentUserContext.currentUserId(),
                subjectId, type, tagIds, page, size));
    }

    /** 详情（§7.2） */
    @GetMapping("/api/entries/{id}")
    public Result<EntryDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(libraryService.detail(CurrentUserContext.currentUserId(), id));
    }

    /** 修改（§7.3）：整体替换式，复习状态不因编辑改变 */
    @PutMapping("/api/entries/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody UpdateEntryDTO req) {
        libraryService.update(CurrentUserContext.currentUserId(), id, req);
        return Result.ok();
    }

    /** 删除（§7.4）：物理删除 + 清图片对象 */
    @DeleteMapping("/api/entries/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        libraryService.delete(CurrentUserContext.currentUserId(), id);
        return Result.ok();
    }
}
