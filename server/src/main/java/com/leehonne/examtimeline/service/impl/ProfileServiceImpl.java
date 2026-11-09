package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehonne.examtimeline.dto.UpdateProfileDTO;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.UserMapper;
import com.leehonne.examtimeline.service.ProfileService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户资料业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper）。
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserMapper userMapper;
    private final StorageService storage;

    @Override
    public ProfileVO get(Long userId) {
        return toVO(require(userId));
    }

    @Override
    @Transactional
    public ProfileVO update(Long userId, UpdateProfileDTO req) {
        User user = require(userId);
        String nickname = req.getNickname();
        String avatarObjectKey = req.getAvatarObjectKey();
        if (nickname == null && (avatarObjectKey == null || avatarObjectKey.isBlank())) {
            throw BusinessException.badRequest("至少修改一项");
        }
        if (nickname != null) {
            String n = nickname.trim();
            if (n.isEmpty() || n.length() > 20) {
                throw BusinessException.badRequest("昵称需 1~20 个字符");
            }
            user.setNickname(n);
        }
        if (avatarObjectKey != null && !avatarObjectKey.isBlank()) {
            String key = avatarObjectKey.trim();
            if (!key.startsWith("user_" + userId + "/")) {
                throw BusinessException.badRequest("头像图片不属于当前用户");
            }
            user.setAvatarObjectKey(key);
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    // ---- 内部 ----

    private User require(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.unauthorized("未登录或登录已过期，请先登录");
        }
        return u;
    }

    private ProfileVO toVO(User u) {
        return new ProfileVO(u.getId(), u.getEmail(), u.getNickname(), u.getRole().getCode(), avatarUrl(u), u.getExamYear());
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
