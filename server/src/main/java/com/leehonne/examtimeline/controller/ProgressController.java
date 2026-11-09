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

/**
 * 进度模块接口（契约 §8）：统计页「学习进度总结」。
 */
@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /** 统计（§8.1）：每日学习统计 + 区间内签到日期；from/to 缺省=本月 1 日至今 */
    @GetMapping("/api/progress")
    public Result<ProgressVO> progress(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return Result.ok(progressService.progress(CurrentUserContext.currentUserId(), from, to));
    }
}
