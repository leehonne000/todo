package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.dto.UpdateProfileDTO;
import com.leehonne.examtimeline.vo.ProfileVO;

/**
 * 用户资料业务（契约 §3.5）：头像+昵称。
 * 注册时后端自动赋默认（昵称=邮箱@前缀，头像=系统默认图）。
 */
public interface ProfileService {

    ProfileVO get(Long userId);

    ProfileVO update(Long userId, UpdateProfileDTO req);
}
