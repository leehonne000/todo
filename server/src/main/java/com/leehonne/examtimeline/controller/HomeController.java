package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

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
    public Result<HomeVO> home() {
        return Result.ok(homeService.home(CurrentUserContext.currentUserId()));
    }

    /** 每日签到（§4.2） */
    @PostMapping("/api/check-ins")
    public Result<Void> checkIn() {
        homeService.checkIn(CurrentUserContext.currentUserId());
        return Result.ok();
    }
}
