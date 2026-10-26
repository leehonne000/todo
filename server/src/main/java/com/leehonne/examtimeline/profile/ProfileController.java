package com.leehonne.examtimeline.profile;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
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
    public ApiResponse<ProfileResponse> get() {
        return ApiResponse.ok(profileService.get(SecurityUtils.currentUserId()));
    }

    /** 修改资料（§3.5.2）：nickname / avatarObjectKey 至少一项 */
    @PutMapping
    public ApiResponse<ProfileResponse> update(@RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(profileService.update(SecurityUtils.currentUserId(), req));
    }
}
