package com.leehonne.examtimeline.home;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主界面接口（契约 §4）：状态聚合 + 每日签到。
 */
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /** 主界面聚合（§4.1）：签到态/今日学习态/应复习数 */
    @GetMapping("/api/home")
    public ApiResponse<HomeResponse> home() {
        return ApiResponse.ok(homeService.home(SecurityUtils.currentUserId()));
    }

    /** 每日签到（§4.2） */
    @PostMapping("/api/check-ins")
    public ApiResponse<Void> checkIn() {
        homeService.checkIn(SecurityUtils.currentUserId());
        return ApiResponse.ok();
    }
}
