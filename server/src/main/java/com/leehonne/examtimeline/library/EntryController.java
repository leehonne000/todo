package com.leehonne.examtimeline.library;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.library.dto.EntryDetailResponse;
import com.leehonne.examtimeline.library.dto.EntryListResponse;
import com.leehonne.examtimeline.library.dto.UpdateEntryRequest;
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
    public ApiResponse<EntryListResponse> list(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "tagIds", required = false) String tagIds,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(libraryService.list(SecurityUtils.currentUserId(),
                subjectId, type, tagIds, page, size));
    }

    /** 详情（§7.2） */
    @GetMapping("/api/entries/{id}")
    public ApiResponse<EntryDetailResponse> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(libraryService.detail(SecurityUtils.currentUserId(), id));
    }

    /** 修改（§7.3）：整体替换式，复习状态不因编辑改变 */
    @PutMapping("/api/entries/{id}")
    public ApiResponse<Void> update(@PathVariable("id") Long id, @RequestBody UpdateEntryRequest req) {
        libraryService.update(SecurityUtils.currentUserId(), id, req);
        return ApiResponse.ok();
    }

    /** 删除（§7.4）：物理删除 + 清图片对象 */
    @DeleteMapping("/api/entries/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        libraryService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok();
    }
}
