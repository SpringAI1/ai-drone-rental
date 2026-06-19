package com.drone.rental.controller;

import com.drone.rental.websocket.OrderNotificationHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket 调试接口
 */
@RestController
@RequestMapping("/ws-debug")
public class WebSocketDebugController {

    private final OrderNotificationHandler orderNotificationHandler;

    public WebSocketDebugController(OrderNotificationHandler orderNotificationHandler) {
        this.orderNotificationHandler = orderNotificationHandler;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "onlineAdmins", OrderNotificationHandler.getOnlineCount(),
                "connectUrl", "/api/ws/orders?token=xxx",
                "note", "仅管理员(role=1)可以连接"
        );
    }
}
