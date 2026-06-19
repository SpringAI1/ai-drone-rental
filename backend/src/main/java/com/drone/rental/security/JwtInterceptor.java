package com.drone.rental.security;

import com.drone.rental.common.Result;
import com.drone.rental.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 拦截器
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.prefix:Bearer }")
    private String prefix;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(header);
        if (!StringUtils.hasText(authHeader)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        String token = authHeader;
        if (authHeader.startsWith(prefix)) {
            token = authHeader.substring(prefix.length());
        }
        token = token.trim();

        if (!jwtUtil.validateToken(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);

        if (userId == null || username == null) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        UserContext.setCurrentUser(new UserContext.UserInfo(userId, username, role));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除用户上下文
        UserContext.clear();
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<Void> result = Result.error(resultCode);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
