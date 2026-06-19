package com.drone.rental.config;

import com.drone.rental.websocket.OrderNotificationHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 注册 "/ws/orders" 处理器（结合 context-path="/api" 最终路径为 "/api/ws/orders"）
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderNotificationHandler orderNotificationHandler;

    public WebSocketConfig(OrderNotificationHandler orderNotificationHandler) {
        this.orderNotificationHandler = orderNotificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 同时注册两个路径（一个相对 context-path，一个绝对路径
        registry.addHandler(orderNotificationHandler, "/ws/orders")
                .setAllowedOrigins("*");
    }
}
