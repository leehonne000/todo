package com.leehonne.examtimeline.auth;

import com.leehonne.examtimeline.auth.dto.*;
import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.entity.VerifyCode;
import com.leehonne.examtimeline.enums.VerifyPurpose;
import com.leehonne.examtimeline.repository.SubjectRepository;
import com.leehonne.examtimeline.repository.UserRepository;
import com.leehonne.examtimeline.repository.VerifyCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 注册登录业务：发码 / 注册激活 / 登录 / 找回密码。
 * 规则（docs/数据库设计.md §2.9）：验证码 10 分钟有效、只用一次、同邮箱同用途 60s 限频、purpose 防串用。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_COOL_SECONDS = 60;

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final VerifyCodeRepository verifyCodeRepository;
    private final MailService mailService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    // ---------- 发验证码 ----------

    @Transactional
    public void sendCode(SendCodeRequest req) {
        VerifyPurpose purpose = VerifyPurpose.fromCode(req.getPurpose());
        String email = req.getEmail().trim().toLowerCase();

        if (purpose == VerifyPurpose.REGISTER && userRepository.existsByEmail(email)) {
            throw BizException.conflict("该邮箱已注册，请直接登录");
        }
        if (purpose == VerifyPurpose.RESET_PASSWORD && !userRepository.existsByEmail(email)) {
            throw BizException.notFound("该邮箱未注册");
        }

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusSeconds(RESEND_COOL_SECONDS);
        if (verifyCodeRepository.countByEmailAndPurposeAndCreatedAtAfter(email, purpose, oneMinuteAgo) > 0) {
            throw BizException.tooManyRequests("发送太频繁，请 " + RESEND_COOL_SECONDS + " 秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        VerifyCode vc = new VerifyCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setPurpose(purpose);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        verifyCodeRepository.save(vc);

        String scene = purpose == VerifyPurpose.REGISTER ? "注册激活" : "找回密码";
        mailService.sendVerifyCode(email, code, scene);
    }

    // ---------- 注册（校验激活码 → 建号并置已激活 + 录入科目组合） ----------

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BizException.conflict("该邮箱已注册，请直接登录");
        }
        consumeCode(email, req.getCode(), VerifyPurpose.REGISTER);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEmailVerified(true);   // 激活码校验成功即激活
        user.setExamYear(req.getExamYear());
        userRepository.save(user);

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
            subjectRepository.save(subject);
        }

        return new LoginResponse(jwtService.issue(user.getId()), user.getId(), user.getEmail());
    }

    // ---------- 登录 ----------

    public LoginResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BizException.unauthorized("邮箱或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BizException.unauthorized("邮箱或密码错误");
        }
        if (!user.getEmailVerified()) {
            throw BizException.forbidden("邮箱未激活，请先完成注册激活");
        }
        return new LoginResponse(jwtService.issue(user.getId()), user.getId(), user.getEmail());
    }

    // ---------- 找回密码 ----------

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BizException.notFound("该邮箱未注册"));
        consumeCode(email, req.getCode(), VerifyPurpose.RESET_PASSWORD);
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ---------- 内部：取最新有效码比对并消费 ----------

    private void consumeCode(String email, String code, VerifyPurpose purpose) {
        VerifyCode vc = verifyCodeRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterOrderByIdDesc(
                        email, purpose, LocalDateTime.now())
                .orElseThrow(() -> BizException.badRequest("验证码无效或已过期，请重新获取"));
        if (!vc.getCode().equals(code.trim())) {
            throw BizException.badRequest("验证码错误");
        }
        vc.setUsedAt(LocalDateTime.now());
        verifyCodeRepository.save(vc);
    }
}
