package com.leehonne.examtimeline.common.jwt;

import com.leehonne.examtimeline.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（课程第 16 课 JwtUtil）—— jjwt 0.12.x 三件套，HS256 签名。
 * Token 携带 userId/email/role（subject=userId，兼容旧契约）；密钥放配置不放代码。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireHours;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    /** 生成 Token：subject=userId，claims 带 email/role */
    public String generateToken(LoginUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /** 解析 Token 得到当前用户；签名/过期不合法返回 null */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            Integer role = claims.get("role", Integer.class);
            return new LoginUser(userId, email, role);
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验 Token 是否有效（未过期且签名正确） */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
