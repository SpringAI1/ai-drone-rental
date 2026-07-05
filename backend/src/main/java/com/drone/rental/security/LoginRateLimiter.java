package com.drone.rental.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存登录速率限制器
 * <p>
 * 限制同一 IP 在时间窗口内的登录尝试次数，防止暴力破解。
 * 生产环境建议替换为 Redis 版本，支持分布式部署。
 */
@Slf4j
@Component
public class LoginRateLimiter {

    /** 每个 IP 的最大尝试次数 */
    private static final int MAX_ATTEMPTS = 10;
    /** 时间窗口（毫秒） */
    private static final long WINDOW_MS = 60_000;
    /** 封禁时间（毫秒） */
    private static final long BLOCK_DURATION_MS = 300_000;

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    private record AttemptWindow(int count, long windowStart, long blockedUntil) {}

    /**
     * 检查是否允许该 IP 尝试登录
     * @return true 表示允许，false 表示被限流
     */
    public boolean allowAttempt(HttpServletRequest request) {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        AttemptWindow window = attempts.compute(ip, (k, existing) -> {
            if (existing == null) {
                return new AttemptWindow(1, now, 0);
            }
            // 检查是否在封禁期
            if (existing.blockedUntil > now) {
                return existing; // 仍在封禁中，不重置计数
            }
            // 检查时间窗口是否过期
            if (now - existing.windowStart > WINDOW_MS) {
                return new AttemptWindow(1, now, 0);
            }
            int newCount = existing.count + 1;
            if (newCount > MAX_ATTEMPTS) {
                log.warn("IP {} 登录尝试超限，封禁 {} 分钟", ip, BLOCK_DURATION_MS / 60000);
                return new AttemptWindow(newCount, existing.windowStart, now + BLOCK_DURATION_MS);
            }
            return new AttemptWindow(newCount, existing.windowStart, 0);
        });

        if (window.blockedUntil > now) {
            log.warn("IP {} 处于封禁期，拒绝登录请求", ip);
            return false;
        }
        return true;
    }

    /**
     * 登录成功后重置该 IP 的计数
     */
    public void resetAttempt(HttpServletRequest request) {
        String ip = getClientIp(request);
        attempts.remove(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
