package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.common.Result;
import com.drone.rental.entity.Notification;
import com.drone.rental.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "消息通知-用户端")
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public Result<IPage<Notification>> getNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Notification> page = notificationService.getCurrentUserNotifications(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return Result.success();
    }

    // ========== 管理员 ======================================
    @Operation(summary = "管理员-获取所有通知")
    @GetMapping("/admin/list")
    public Result<IPage<Notification>> getAdminNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "通知类型: 1-订单 2-评论 3-故障 4-空域 5-系统") @RequestParam(required = false) Integer type,
            @Parameter(description = "仅看管理员通知") @RequestParam(defaultValue = "true") Boolean adminOnly) {
        IPage<Notification> page = notificationService.getAdminNotifications(pageNum, pageSize, type, adminOnly);
        return Result.success(page);
    }

    @Operation(summary = "管理员-未读通知数量")
    @GetMapping("/admin/unread-count")
    public Result<Map<String, Object>> getAdminUnreadCount() {
        Long count = notificationService.getAdminUnreadCount();
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @Operation(summary = "管理员-标记通知为已读")
    @PutMapping("/admin/{id}/read")
    public Result<Void> adminMarkAsRead(@PathVariable Long id) {
        notificationService.adminMarkAsRead(id);
        return Result.success();
    }

    @Operation(summary = "管理员-全部标记为已读")
    @PutMapping("/admin/read-all")
    public Result<Void> adminMarkAllAsRead() {
        notificationService.adminMarkAllAsRead();
        return Result.success();
    }

    /**
     * 管理员一键清空所有通知（物理删除）
     */
    @DeleteMapping("/admin/clear")
    public Result<Void> adminClearAll() {
        notificationService.adminClearAll();
        return Result.success();
    }
}