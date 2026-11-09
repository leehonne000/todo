package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

import com.leehonne.examtimeline.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信息模块接口（契约 §9）：固定信息区 + 博客内容区（用户只读）。
 */
@RestController
@RequiredArgsConstructor
public class InfoController {

    private final InfoService infoService;

    /** 固定信息区（§9.1）：倒计时 + 硬节点时间线 */
    @GetMapping("/api/info/overview")
    public Result<InfoOverviewVO> overview() {
        return Result.ok(infoService.overview(CurrentUserContext.currentUserId()));
    }

    /** 博客列表（§9.2）：只含已发布，倒序 */
    @GetMapping("/api/articles")
    public Result<PageVO<ArticleListItemVO>> articles(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.ok(infoService.articles(page, size));
    }

    /** 文章详情（§9.3）：草稿不可见（3002） */
    @GetMapping("/api/articles/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(infoService.detail(id));
    }
}
