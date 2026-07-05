package com.drone.rental.config;

import com.drone.rental.security.JwtUtil;
import com.drone.rental.websocket.OrderNotificationHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 配置
 * 注册 "/ws/orders" 处理器（结合 context-path="/api" 最终路径为 "/api/ws/orders"）
 *
 * 注意：Spring WebSocket 协议升级后 session.getUri() 经常丢失 query string，
 * 因此用 HandshakeInterceptor 在握手阶段就把 token 抓到 session attributes。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderNotificationHandler orderNotificationHandler;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://localhost:8080}")
    private String allowedOrigins;

    public WebSocketConfig(OrderNotificationHandler orderNotificationHandler, JwtUtil jwtUtil) {
        this.orderNotificationHandler = orderNotificationHandler;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderNotificationHandler, "/ws/orders")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(new TokenHandshakeInterceptor());
    }

    /**
     * 在 HTTP 握手阶段从 URL query 抓 token，放到 WebSocketSession attributes。
     * 之后 afterConnectionEstablished 就能从 attributes 读到。
     */
    private class TokenHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            URI uri = request.getURI();
            String query = uri.getQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq > 0 && "token".equals(part.substring(0, eq))) {
                        String token = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                        attributes.put("token", token);
                        try {
                            Long userId = jwtUtil.getUserIdFromToken(token);
                            Integer role = jwtUtil.getRoleFromToken(token);
                            if (userId != null) attributes.put("userId", userId);
                            if (role != null) attributes.put("role", role);
                        } catch (Exception ignored) {
                            // token 解析失败留给 afterConnectionEstablished 处理
                        }
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // no-op
        }
    }
}
