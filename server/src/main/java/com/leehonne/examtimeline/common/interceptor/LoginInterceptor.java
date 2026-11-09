package com.leehonne.examtimeline.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.ResultCode;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.common.context.LoginUser;
import com.leehonne.examtimeline.common.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器（课程第 16 课 LoginInterceptor）—— 统一 JWT 校验第一道防线。
 * 流程：取 Authorization Header → 剥离 Bearer → 解析校验 → 存入 ThreadLocal → 放行/401。
 * afterCompletion 清理 ThreadLocal，防线程复用泄漏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            LoginUser user = jwtUtil.parseToken(token);
            if (user != null) {
                CurrentUserContext.set(user);
                return true;
            }
        }

        writeUnauthorized(response, request.getRequestURI());
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.remove();
    }

    private void writeUnauthorized(HttpServletResponse response, String path) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(ResultCode.UNAUTHORIZED);
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }
}
