package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.VerifyCode;
import com.leehonne.examtimeline.enums.VerifyPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerifyCodeRepository extends JpaRepository<VerifyCode, Long> {

    /** 校验：取最新未过期未使用的码 */
    Optional<VerifyCode> findFirstByEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterOrderByIdDesc(
            String email, VerifyPurpose purpose, LocalDateTime now);

    /** 限频：同邮箱同用途 60 秒内已发过几条 */
    long countByEmailAndPurposeAndCreatedAtAfter(
            String email, VerifyPurpose purpose, LocalDateTime after);
}
