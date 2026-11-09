package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

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
    public Result<List<TagVO>> tags(@RequestParam(value = "q", required = false) String q) {
        return Result.ok(libraryService.tags(CurrentUserContext.currentUserId(), q));
    }
}
