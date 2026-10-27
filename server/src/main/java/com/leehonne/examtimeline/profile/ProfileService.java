package com.leehonne.examtimeline.profile;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.repository.UserRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户资料业务（契约 §3.5）：头像+昵称。
 * 注册时后端自动赋默认（昵称=邮箱@前缀，头像=系统默认图）。
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MinioStorageService storage;

    public ProfileResponse get(Long userId) {
        return toResponse(require(userId));
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest req) {
        User user = require(userId);
        String nickname = req.getNickname();
        String avatarObjectKey = req.getAvatarObjectKey();
        if (nickname == null && (avatarObjectKey == null || avatarObjectKey.isBlank())) {
            throw BizException.badRequest("至少修改一项");
        }
        if (nickname != null) {
            String n = nickname.trim();
            if (n.isEmpty() || n.length() > 20) {
                throw BizException.badRequest("昵称需 1~20 个字符");
            }
            user.setNickname(n);
        }
        if (avatarObjectKey != null && !avatarObjectKey.isBlank()) {
            String key = avatarObjectKey.trim();
            if (!key.startsWith("user_" + userId + "/")) {
                throw BizException.badRequest("头像图片不属于当前用户");
            }
            user.setAvatarObjectKey(key);
        }
        userRepository.save(user);
        return toResponse(user);
    }

    // ---- 内部 ----

    private User require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("未登录或登录已过期，请先登录"));
    }

    private ProfileResponse toResponse(User u) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getNickname(), u.getRole().getCode(), avatarUrl(u), u.getExamYear());
    }

    /** 头像地址：空 → 默认图；/ 或 http 开头（静态资源）原样；否则对象键 → 预签名 */
    private String avatarUrl(User u) {
        String key = u.getAvatarObjectKey();
        if (key == null || key.isBlank()) {
            return "/avatar-default.png";
        }
        if (key.startsWith("/") || key.startsWith("http")) {
            return key;
        }
        return storage.signedUrl(key);
    }
}
