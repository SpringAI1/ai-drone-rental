package com.drone.rental.service.support;

import com.drone.rental.entity.Drone;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.entity.User;
import com.drone.rental.service.NotificationService;
import com.drone.rental.service.UserService;
import com.drone.rental.websocket.OrderNotificationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单通知工具：下单后向用户/管理员发站内信 + WebSocket 广播。
 * 从 OrderServiceImpl 抽出以降低类大小。
 */
@Slf4j
@Component
public class OrderNotifier {

    private final NotificationService notificationService;
    private final UserService userService;
    private final OrderNotificationHandler orderNotificationHandler;

    public OrderNotifier(NotificationService notificationService,
                         UserService userService,
                         OrderNotificationHandler orderNotificationHandler) {
        this.notificationService = notificationService;
        this.userService = userService;
        this.orderNotificationHandler = orderNotificationHandler;
    }

    /**
     * 发送"订单创建成功"通知：用户站内信 + 管理员站内信 + WebSocket 广播。
     * 任意一步失败都不影响主流程（订单已落库）。
     */
    public void sendCreateNotifications(RentalOrder order, Drone drone, BigDecimal totalAmount) {
        Long userId = order.getUserId();
        String droneModel = drone.getModel() != null ? drone.getModel() : "无人机";

        // 1) 用户通知
        try {
            String title = "订单创建成功";
            String content = "您已成功下单「" + droneModel + "」，订单号 " + order.getOrderNo()
                    + "，订单金额 ¥" + totalAmount + "，请尽快完成支付。";
            notificationService.sendNotification(userId, 1, title, content, order.getId());
        } catch (Exception e) {
            log.warn("发送用户通知失败: {}", e.getMessage());
        }

        // 2) 管理员通知 + WebSocket 广播
        try {
            User user = userService.getById(userId);
            String userName = user != null && user.getUsername() != null ? user.getUsername() : "用户";
            String adminTitle = "新订单待处理";
            String adminContent = "「" + userName + "」下单了「" + droneModel
                    + "」，订单号 " + order.getOrderNo() + "，金额 ¥" + totalAmount + "，请及时处理。";
            // userId=-1 表示管理员通知
            notificationService.sendNotification(-1L, 1, adminTitle, adminContent, order.getId());

            // WebSocket 广播到在线管理员
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "new_order");
                payload.put("orderId", order.getId());
                payload.put("orderNo", order.getOrderNo());
                payload.put("totalAmount", totalAmount.toString());
                payload.put("droneModel", droneModel);
                payload.put("userName", userName);
                payload.put("title", adminTitle);
                payload.put("content", adminContent);
                payload.put("createdAt", LocalDateTime.now().toString());
                orderNotificationHandler.broadcast(payload);
            } catch (Exception e) {
                log.warn("WS 广播失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("发送管理员通知失败: {}", e.getMessage());
        }
    }
}
