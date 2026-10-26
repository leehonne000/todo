package com.leehonne.examtimeline.learn;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.learn.dto.FinishSessionRequest;
import com.leehonne.examtimeline.learn.dto.SaveEntriesRequest;
import com.leehonne.examtimeline.learn.dto.StartSessionRequest;
import com.leehonne.examtimeline.learn.dto.StartSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习 session 接口（契约 §5.2–§5.4）：开始计时 → 长按结束 → Done 批量落库。
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final LearnService learnService;

    /** 开始计时（§5.2）：选完科目进横屏那一刻建行 */
    @PostMapping
    public ApiResponse<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
        return ApiResponse.ok(learnService.startSession(SecurityUtils.currentUserId(), req.getSubjectId()));
    }

    /** 长按结束（§5.3）：写净学习秒数（排除暂停） */
    @PostMapping("/{id}/finish")
    public ApiResponse<Void> finish(@PathVariable("id") Long id,
                                    @Valid @RequestBody FinishSessionRequest req) {
        learnService.finishSession(SecurityUtils.currentUserId(), id, req.getDurationSeconds());
        return ApiResponse.ok();
    }

    /** Done（§5.4）：本次全部 note/错题同事务落库，每条自动进艾宾浩斯队列 */
    @PostMapping("/{id}/entries")
    public ApiResponse<List<Long>> saveEntries(@PathVariable("id") Long id,
                                               @Valid @RequestBody SaveEntriesRequest req) {
        return ApiResponse.ok(learnService.saveEntries(SecurityUtils.currentUserId(), id, req.getEntries()));
    }
}
