package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口（契约 §3.5）：自定义页头像+昵称。
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 获取资料（§3.5.1） */
    @GetMapping
    public Result<ProfileVO> get() {
        return Result.ok(profileService.get(CurrentUserContext.currentUserId()));
    }

    /** 修改资料（§3.5.2）：nickname / avatarObjectKey 至少一项 */
    @PutMapping
    public Result<ProfileVO> update(@RequestBody UpdateProfileDTO req) {
        return Result.ok(profileService.update(CurrentUserContext.currentUserId(), req));
    }
}
