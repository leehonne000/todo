package com.leehonne.examtimeline.review;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.review.dto.ReviewCompleteResponse;
import com.leehonne.examtimeline.review.dto.ReviewItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Review 复习队列接口（契约 §6）：今日到期列表 + 复习完成推进一轮。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 今日到期列表（§6.1）：Review 卡点击进沉浸式复习 */
    @GetMapping("/due")
    public ApiResponse<List<ReviewItemResponse>> due() {
        return ApiResponse.ok(reviewService.due(SecurityUtils.currentUserId()));
    }

    /** 复习完成（§6.2）：无自判，完成即推进一轮；六轮走完出队 */
    @PostMapping("/{entryId}/complete")
    public ApiResponse<ReviewCompleteResponse> complete(@PathVariable("entryId") Long entryId) {
        return ApiResponse.ok(reviewService.complete(SecurityUtils.currentUserId(), entryId));
    }
}
