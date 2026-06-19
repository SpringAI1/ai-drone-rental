package com.drone.rental.controller;

import com.drone.rental.common.Constants;
import com.drone.rental.common.Result;
import com.drone.rental.dto.AirspaceRecordDTO;
import com.drone.rental.entity.AirspaceRecord;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.AirspaceRecordService;
import com.drone.rental.service.NotificationService;
import com.drone.rental.websocket.OrderNotificationHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "空域备案-用户端")
@RestController
@RequestMapping("/airspace")
@RequiredArgsConstructor
public class AirspaceController {

    private final AirspaceRecordService airspaceRecordService;
    private final NotificationService notificationService;
    private final OrderNotificationHandler notificationHandler;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    );

    private static LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        String str = value.toString().trim();
        if (str.isEmpty()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(str, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Integer parseInt(Object value, Integer def) {
        if (value == null) return def;
        try {
            if (value instanceof Number) return ((Number) value).intValue();
            String str = value.toString().trim();
            if (str.isEmpty()) return def;
            // 处理 120.0 这样的字符串
            int dot = str.indexOf('.');
            if (dot > 0) str = str.substring(0, dot);
            return Integer.parseInt(str);
        } catch (Exception e) {
            return def;
        }
    }

    private static BigDecimal parseBigDecimal(Object value, BigDecimal def) {
        if (value == null) return def;
        try {
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
            String str = value.toString().trim();
            if (str.isEmpty()) return def;
            return new BigDecimal(str);
        } catch (Exception e) {
            return def;
        }
    }

    @Operation(summary = "提交空域备案（兼容多种时间格式）")
    @PostMapping("/submit")
    public Result<Void> submitAirspaceRecord(@RequestBody Map<String, Object> request) {
        String regionName = (String) request.get("regionName");
        if (!StringUtils.hasText(regionName)) {
            return Result.error("飞行区域名称不能为空");
        }

        String regionAddress = (String) request.get("regionAddress");
        BigDecimal longitude = parseBigDecimal(request.get("longitude"), null);
        BigDecimal latitude = parseBigDecimal(request.get("latitude"), null);
        Integer radius = parseInt(request.get("radius"), 500);
        Integer maxAltitude = parseInt(request.get("maxAltitude"), null);
        LocalDateTime plannedStartTime = parseDateTime(request.get("plannedStartTime"));
        LocalDateTime plannedEndTime = parseDateTime(request.get("plannedEndTime"));
        String purpose = (String) request.get("purpose");

        if (maxAltitude == null || maxAltitude <= 0) {
            return Result.error("请输入有效的最大飞行高度");
        }
        if (plannedStartTime == null) {
            return Result.error("请输入有效的开始时间，格式：2025-01-01 10:00");
        }
        if (plannedEndTime == null) {
            return Result.error("请输入有效的结束时间，格式：2025-01-01 18:00");
        }
        if (plannedEndTime.isBefore(plannedStartTime)) {
            return Result.error("结束时间不能早于开始时间");
        }

        Long userId = UserContext.getCurrentUserId();
        AirspaceRecord record = new AirspaceRecord();
        record.setUserId(userId);
        record.setRegionName(regionName);
        record.setRegionAddress(regionAddress);
        record.setLongitude(longitude);
        record.setLatitude(latitude);
        record.setRadius(radius);
        record.setMaxAltitude(maxAltitude);
        record.setPlannedStartTime(plannedStartTime);
        record.setPlannedEndTime(plannedEndTime);
        record.setPurpose(purpose);
        record.setAuditStatus(Constants.AUDIT_STATUS_PENDING);

        try {
            airspaceRecordService.save(record);

            // 广播新的空域备案通知给管理员
            try {
                String content = "区域：" + regionName + "，目的：" + (purpose != null && purpose.length() > 20 ? purpose.substring(0, 20) + "..." : purpose);
                notificationService.sendNotification(-1L, 4, "空域备案待审核", content, record.getId());

                Map<String, Object> airspaceInfo = new HashMap<>();
                airspaceInfo.put("id", record.getId());
                airspaceInfo.put("userId", userId);
                airspaceInfo.put("regionName", regionName);
                airspaceInfo.put("content", content);
                airspaceInfo.put("title", "新的空域备案申请");
                notificationHandler.notifyNewAirspace(airspaceInfo);
            } catch (Exception e) {
                // 通知失败不影响主流程
            }

            return Result.success();
        } catch (Exception e) {
            log.error("空域备案保存失败", e);
            return Result.error("备案信息保存失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户的空域备案列表")
    @GetMapping("/list")
    public Result<List<AirspaceRecord>> getCurrentUserAirspaceRecords() {
        List<AirspaceRecord> list = airspaceRecordService.getCurrentUserAirspaceRecords();
        return Result.success(list);
    }

    @Operation(summary = "获取当前用户已通过审核的空域备案")
    @GetMapping("/approved")
    public Result<List<AirspaceRecord>> getCurrentUserApprovedRecords() {
        List<AirspaceRecord> list = airspaceRecordService.getCurrentUserApprovedRecords();
        return Result.success(list);
    }
}
