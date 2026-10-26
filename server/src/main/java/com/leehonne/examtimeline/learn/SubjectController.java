package com.leehonne.examtimeline.learn;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.learn.dto.SubjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 科目接口（契约 §5.1）：Learn 入口选科目四选一。
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final LearnService learnService;

    /** 注册时定的科目组合，按 sortOrder */
    @GetMapping
    public ApiResponse<List<SubjectResponse>> list() {
        return ApiResponse.ok(learnService.subjects(SecurityUtils.currentUserId()));
    }
}
