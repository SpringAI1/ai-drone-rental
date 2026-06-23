package com.drone.rental.websocket;

import com.drone.rental.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单实时通知 WebSocket 处理器（Spring WebSocket 原生）
 *
 * 前端（管理员）：new WebSocket("ws://host:port/api/ws/orders?token=xxx")
 */
@Component
public class OrderNotificationHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationHandler.class);

    // sessionId -> session（所有在线管理员）
    private static final Map<String, WebSocketSession> ADMIN_SESSIONS = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public OrderNotificationHandler(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 优先从 HandshakeInterceptor 放在 attributes 里的 token 拿（Spring WebSocket 协议升级后 session.getUri() 经常丢失 query string）
        String token = (String) session.getAttributes().get("token");
        if (token == null || token.isEmpty()) {
            token = extractToken(session);
        }
        if (token == null || token.isEmpty()) {
            log.warn("[WS] 握手未携带 token, uri={}", session.getUri());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing token"));
            return;
        }

        try {
            Integer role = jwtUtil.getRoleFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (role == null || role != 1) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("admin only"));
                return;
            }

            ADMIN_SESSIONS.put(session.getId(), session);
            session.getAttributes().put("userId", userId);
            log.info("[WS] 管理员 {} 已连接，当前 {} 人在线", userId, ADMIN_SESSIONS.size());

            sendText(session, Map.of(
                    "type", "connected",
                    "message", "实时订单通知已连接"
            ));
        } catch (Exception e) {
            log.warn("[WS] 鉴权失败: {}", e.getMessage());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            sendText(session, Map.of("type", "pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ADMIN_SESSIONS.remove(session.getId());
        Object userId = session.getAttributes().get("userId");
        log.info("[WS] 管理员 {} 已断开，状态 {}，当前 {} 人在线", userId, status, ADMIN_SESSIONS.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        ADMIN_SESSIONS.remove(session.getId());
        log.warn("[WS] 连接出错: {}", exception.getMessage());
    }

    /**
     * 广播订单消息给所有在线管理员
     */
    public void broadcast(Map<String, Object> payload) {
        if (ADMIN_SESSIONS.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[WS] JSON 序列化失败: {}", e.getMessage());
            return;
        }
        TextMessage msg = new TextMessage(json);

        for (Map.Entry<String, WebSocketSession> entry : ADMIN_SESSIONS.entrySet()) {
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (Exception e) {
                    log.warn("[WS] 广播失败 session={}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        log.info("[WS] 已向 {} 个管理员广播: {}", ADMIN_SESSIONS.size(), payload);
    }

    /**
     * 新评论通知（用户提交新评论后调用）
     */
    public void notifyNewComment(Map<String, Object> commentInfo) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "new_comment");
        payload.put("title", commentInfo.getOrDefault("title", "新的待审核评论"));
        payload.put("content", commentInfo.getOrDefault("content", ""));
        payload.put("userId", commentInfo.getOrDefault("userId", ""));
        payload.put("droneName", commentInfo.getOrDefault("droneName", ""));
        payload.put("commentId", commentInfo.getOrDefault("id", null));
        payload.put("createdAt", java.time.LocalDateTime.now().toString());
        broadcast(payload);
    }

    /**
     * 故障报修通知（用户提交故障报修后调用）
     */
    public void notifyNewFault(Map<String, Object> faultInfo) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "new_fault");
        payload.put("title", faultInfo.getOrDefault("title", "新的故障报修"));
        payload.put("content", faultInfo.getOrDefault("description", ""));
        payload.put("userId", faultInfo.getOrDefault("userId", null));
        payload.put("droneName", faultInfo.getOrDefault("droneModel", ""));
        payload.put("faultId", faultInfo.getOrDefault("id", null));
        payload.put("createdAt", java.time.LocalDateTime.now().toString());
        broadcast(payload);
    }

    /**
     * 空域备案通知
     */
    public void notifyNewAirspace(Map<String, Object> airspaceInfo) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "new_airspace");
        payload.put("title", airspaceInfo.getOrDefault("title", "新的空域备案申请"));
        payload.put("content", airspaceInfo.getOrDefault("content", ""));
        payload.put("userId", airspaceInfo.getOrDefault("userId", ""));
        payload.put("createdAt", java.time.LocalDateTime.now().toString());
        broadcast(payload);
    }

    /**
     * 通用通知
     */
    public void notifyGeneral(String type, String title, String content) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", type);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("createdAt", java.time.LocalDateTime.now().toString());
        broadcast(payload);
    }

    public static int getOnlineCount() {
        return ADMIN_SESSIONS.size();
    }

    private String extractToken(WebSocketSession session) {
        try {
            URI uri = session.getUri();
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

    private void sendText(WebSocketSession session, Map<String, Object> payload) throws Exception {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}
