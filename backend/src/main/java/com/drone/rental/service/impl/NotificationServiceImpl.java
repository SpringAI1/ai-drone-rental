package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.ResultCode;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.entity.Notification;
import com.drone.rental.mapper.NotificationMapper;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public void sendNotification(Long userId, Integer type, String title, String content, Long businessId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBusinessId(businessId);
        notification.setReadStatus(0);
        this.save(notification);
    }

    @Override
    public IPage<Notification> getCurrentUserNotifications(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        return this.page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getDeleted, 0)
                .orderByDesc(Notification::getCreatedTime));
    }

    @Override
    public Long getUnreadCount() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return 0L;
        }

        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, 0)
                .eq(Notification::getDeleted, 0));
    }

    @Override
    public void markAsRead(Long id) {
        Long userId = UserContext.getCurrentUserId();
        Notification notification = this.getById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException("通知不存在");
        }
        notification.setReadStatus(1);
        this.updateById(notification);
    }

    @Override
    public void markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return;
        }

        // 管理员登录时，"全部已读"作用于发给管理员的通知（userId = -1）
        // 普通用户登录时，作用于自己的通知
        Long targetUserId = UserContext.isAdmin() ? -1L : userId;

        this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, targetUserId)
                .eq(Notification::getReadStatus, 0)
                .eq(Notification::getDeleted, 0)
                .set(Notification::getReadStatus, 1));
    }

    // ================ 管理员 ================
    @Override
    public IPage<Notification> getAdminNotifications(Integer pageNum, Integer pageSize, Integer type, Boolean adminOnly) {
        LambdaQueryWrapper<Notification> q = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getDeleted, 0);
        if (adminOnly != null && adminOnly) {
            q.eq(Notification::getUserId, -1);
        }
        if (type != null && type > 0) {
            q.eq(Notification::getType, type);
        }
        q.orderByDesc(Notification::getCreatedTime);
        return this.page(new Page<>(pageNum, pageSize), q);
    }

    @Override
    public Long getAdminUnreadCount() {
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, -1)
                .eq(Notification::getReadStatus, 0)
                .eq(Notification::getDeleted, 0));
    }

    @Override
    public void adminMarkAsRead(Long id) {
        Notification n = this.getById(id);
        if (n == null) {
            throw new BusinessException("通知不存在");
        }
        n.setReadStatus(1);
        this.updateById(n);
    }

    @Override
    public void adminMarkAllAsRead() {
        this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, -1)
                .eq(Notification::getReadStatus, 0)
                .eq(Notification::getDeleted, 0)
                .set(Notification::getReadStatus, 1));
    }

    @Override
    public void adminClearAll() {
        // 物理删除管理员收到的所有通知（绕过逻辑删除）
        this.baseMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, -1));
    }
}