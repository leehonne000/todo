package com.leehonne.examtimeline.info;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.info.dto.ArticleDetailResponse;
import com.leehonne.examtimeline.info.dto.ArticleListResponse;
import com.leehonne.examtimeline.info.dto.InfoOverviewResponse;
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
    public ApiResponse<InfoOverviewResponse> overview() {
        return ApiResponse.ok(infoService.overview(SecurityUtils.currentUserId()));
    }

    /** 博客列表（§9.2）：只含已发布，倒序 */
    @GetMapping("/api/articles")
    public ApiResponse<ArticleListResponse> articles(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(infoService.articles(page, size));
    }

    /** 文章详情（§9.3）：草稿不可见（3002） */
    @GetMapping("/api/articles/{id}")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(infoService.detail(id));
    }
}
