package com.leehonne.examtimeline.library;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.library.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 标签词表接口（契约 §7.5） */
@RestController
@RequiredArgsConstructor
public class TagController {

    private final LibraryService libraryService;

    /** 标签词表（§7.5）：q 按名称过滤（可选） */
    @GetMapping("/api/tags")
    public ApiResponse<List<TagResponse>> tags(@RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(libraryService.tags(SecurityUtils.currentUserId(), q));
    }
}
