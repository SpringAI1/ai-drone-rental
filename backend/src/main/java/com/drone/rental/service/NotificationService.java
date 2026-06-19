package com.drone.rental.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.entity.Notification;

import java.util.Map;

public interface NotificationService extends IService<Notification> {

    void sendNotification(Long userId, Integer type, String title, String content, Long businessId);

    IPage<Notification> getCurrentUserNotifications(Integer pageNum, Integer pageSize);

    Long getUnreadCount();

    void markAsRead(Long id);

    void markAllAsRead();

    // ===== 管理员 =====
    IPage<Notification> getAdminNotifications(Integer pageNum, Integer pageSize, Integer type, Boolean adminOnly);

    Long getAdminUnreadCount();

    void adminMarkAsRead(Long id);

    void adminMarkAllAsRead();
}