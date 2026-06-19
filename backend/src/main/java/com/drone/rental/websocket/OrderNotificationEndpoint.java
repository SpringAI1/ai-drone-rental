package com.drone.rental.websocket;

import com.drone.rental.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单实时通知 WebSocket 端点
 * URL: ws://host:port/api/ws/orders?token=xxx
 *
 * 只允许管理员（role=1）连接，创建订单时向所有在线管理员广播
 */
@Component
@ServerEndpoint("/api/ws/orders")
public class OrderNotificationEndpoint {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationEndpoint.class);

    private static final Map<String, Session> ADMIN_SESSIONS = new ConcurrentHashMap<>();

    // 通过 Spring 上下文 holder 注入 —— 在 onOpen 时 lazy 获取
    private static ApplicationContext applicationContext;
    private static JwtUtil jwtUtil;
    private static ObjectMapper objectMapper;

    /**
     * Spring 注入：首次连接时懒加载
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    private static synchronized void ensureBeans() {
        if (jwtUtil == null || objectMapper == null) {
            if (applicationContext != null) {
                jwtUtil = applicationContext.getBean(JwtUtil.class);
                objectMapper = applicationContext.getBean(ObjectMapper.class);
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        ensureBeans();

        String token = extractToken(session);
        if (token == null || token.isEmpty()) {
            closeSession(session, "缺少 token");
            return;
        }

        try {
            if (jwtUtil == null) {
                closeSession(session, "系统初始化中");
                return;
            }
            Integer role = jwtUtil.getRoleFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (role == null || role != 1) {
                closeSession(session, "仅管理员可连接");
                return;
            }

            ADMIN_SESSIONS.put(session.getId(), session);
            session.getUserProperties().put("userId", userId);
            log.info("[WS] 管理员 {} 已连接，当前 {} 人在线", userId, ADMIN_SESSIONS.size());

            sendText(session, Map.of(
                    "type", "connected",
                    "message", "实时订单通知已连接"
            ));
        } catch (Exception e) {
            log.warn("[WS] 鉴权失败: {}", e.getMessage());
            closeSession(session, "token 无效");
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if ("ping".equalsIgnoreCase(message)) {
            ensureBeans();
            sendText(session, Map.of("type", "pong"));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        ADMIN_SESSIONS.remove(session.getId());
        Object userId = session.getUserProperties().get("userId");
        log.info("[WS] 管理员 {} 已断开，原因: {}，当前 {} 人在线",
                userId, reason, ADMIN_SESSIONS.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        ADMIN_SESSIONS.remove(session.getId());
        log.warn("[WS] 连接出错: {}", throwable.getMessage());
    }

    /**
     * 广播一条新订单消息给所有在线管理员
     */
    public static void broadcast(Map<String, Object> payload) {
        ensureBeans();
        if (ADMIN_SESSIONS.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[WS] JSON 序列化失败: {}", e.getMessage());
            return;
        }

        for (Map.Entry<String, Session> entry : ADMIN_SESSIONS.entrySet()) {
            Session s = entry.getValue();
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.warn("[WS] 广播失败 session={}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        log.info("[WS] 已向 {} 个管理员广播: {}", ADMIN_SESSIONS.size(), payload);
    }

    public static int getOnlineCount() {
        return ADMIN_SESSIONS.size();
    }

    private String extractToken(Session session) {
        try {
            URI uri = session.getRequestURI();
            if (uri != null) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String part : query.split("&")) {
                        int eq = part.indexOf('=');
                        if (eq > 0 && "token".equals(part.substring(0, eq))) {
                            return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendText(Session session, Map<String, Object> payload) {
        if (!session.isOpen()) return;
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("[WS] 发送失败: {}", e.getMessage());
        }
    }

    private void closeSession(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException e) {
            // ignore
        }
    }
}
