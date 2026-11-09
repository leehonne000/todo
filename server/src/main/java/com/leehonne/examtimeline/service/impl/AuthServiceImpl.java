package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehonne.examtimeline.common.context.LoginUser;
import com.leehonne.examtimeline.common.jwt.JwtUtil;
import com.leehonne.examtimeline.dto.LoginDTO;
import com.leehonne.examtimeline.dto.RegisterDTO;
import com.leehonne.examtimeline.dto.ResetPasswordDTO;
import com.leehonne.examtimeline.dto.SendCodeDTO;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.entity.VerifyCode;
import com.leehonne.examtimeline.enums.VerifyPurpose;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.SubjectMapper;
import com.leehonne.examtimeline.mapper.UserMapper;
import com.leehonne.examtimeline.mapper.VerifyCodeMapper;
import com.leehonne.examtimeline.service.AuthService;
import com.leehonne.examtimeline.service.MailService;
import com.leehonne.examtimeline.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 注册登录业务实现：发码 / 注册激活 / 登录 / 找回密码。
 * 持久层 MyBatis-Plus：BaseMapper + LambdaQueryWrapper。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_COOL_SECONDS = 60;

    private final UserMapper userMapper;
    private final SubjectMapper subjectMapper;
    private final VerifyCodeMapper verifyCodeMapper;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    // ---------- 发验证码 ----------

    @Override
    @Transactional
    public void sendCode(SendCodeDTO req) {
        VerifyPurpose purpose = VerifyPurpose.fromCode(req.getPurpose());
        String email = req.getEmail().trim().toLowerCase();

        if (purpose == VerifyPurpose.REGISTER && existsByEmail(email)) {
            throw BusinessException.conflict("该邮箱已注册，请直接登录");
        }
        if (purpose == VerifyPurpose.RESET_PASSWORD && !existsByEmail(email)) {
            throw BusinessException.notFound("该邮箱未注册");
        }

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusSeconds(RESEND_COOL_SECONDS);
        long recent = verifyCodeMapper.selectCount(new LambdaQueryWrapper<VerifyCode>()
                .eq(VerifyCode::getEmail, email)
                .eq(VerifyCode::getPurpose, purpose)
                .gt(VerifyCode::getCreatedAt, oneMinuteAgo));
        if (recent > 0) {
            throw BusinessException.tooManyRequests("发送太频繁，请 " + RESEND_COOL_SECONDS + " 秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        VerifyCode vc = new VerifyCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setPurpose(purpose);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        verifyCodeMapper.insert(vc);

        String scene = purpose == VerifyPurpose.REGISTER ? "注册激活" : "找回密码";
        mailService.sendVerifyCode(email, code, scene);
    }

    // ---------- 注册（校验激活码 → 建号并置已激活 + 录入科目组合） ----------

    @Override
    @Transactional
    public LoginVO register(RegisterDTO req) {
        String email = req.getEmail().trim().toLowerCase();
        if (existsByEmail(email)) {
            throw BusinessException.conflict("该邮箱已注册，请直接登录");
        }
        consumeCode(email, req.getCode(), VerifyPurpose.REGISTER);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEmailVerified(true);   // 激活码校验成功即激活
        user.setExamYear(req.getExamYear());
        // 默认昵称 = 邮箱 @ 前缀（§3.5）；头像留空 = 系统默认头像
        user.setNickname(email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
        userMapper.insert(user);

        List<String> names = req.getSubjects().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        for (int i = 0; i < names.size(); i++) {
            Subject subject = new Subject();
            subject.setUserId(user.getId());
            subject.setName(names.get(i));
            subject.setSortOrder(i);
            subjectMapper.insert(subject);
        }

        return toLoginVO(user);
    }

    // ---------- 登录 ----------

    @Override
    public LoginVO login(LoginDTO req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = findByEmail(email)
                .orElseThrow(() -> BusinessException.unauthorized("邮箱或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("邮箱或密码错误");
        }
        if (!user.getEmailVerified()) {
            throw BusinessException.forbidden("邮箱未激活，请先完成注册激活");
        }
        return toLoginVO(user);
    }

    // ---------- 找回密码 ----------

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDTO req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("该邮箱未注册"));
        consumeCode(email, req.getCode(), VerifyPurpose.RESET_PASSWORD);
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    // ---------- 内部 ----------

    private boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    private java.util.Optional<User> findByEmail(String email) {
        return java.util.Optional.ofNullable(userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)));
    }

    /** 取最新有效码比对并消费 */
    private void consumeCode(String email, String code, VerifyPurpose purpose) {
        VerifyCode vc = verifyCodeMapper.selectOne(new LambdaQueryWrapper<VerifyCode>()
                .eq(VerifyCode::getEmail, email)
                .eq(VerifyCode::getPurpose, purpose)
                .isNull(VerifyCode::getUsedAt)
                .gt(VerifyCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(VerifyCode::getId)
                .last("LIMIT 1"));
        if (vc == null) {
            throw BusinessException.badRequest("验证码无效或已过期，请重新获取");
        }
        if (!vc.getCode().equals(code.trim())) {
            throw BusinessException.badRequest("验证码错误");
        }
        vc.setUsedAt(LocalDateTime.now());
        verifyCodeMapper.updateById(vc);
    }

    private LoginVO toLoginVO(User user) {
        LoginUser loginUser = new LoginUser(user.getId(), user.getEmail(), user.getRole().getCode());
        return new LoginVO(jwtUtil.generateToken(loginUser), user.getId(), user.getEmail(), user.getRole().getCode());
    }
}
