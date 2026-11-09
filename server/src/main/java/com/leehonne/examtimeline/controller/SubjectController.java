package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

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
    public Result<List<SubjectVO>> list() {
        return Result.ok(learnService.subjects(CurrentUserContext.currentUserId()));
    }
}
