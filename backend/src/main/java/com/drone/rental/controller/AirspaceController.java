package com.drone.rental.controller;

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
        // 构建 DTO 并委托给 Service 层（不再绕过 Service 直接操作 entity）
        AirspaceRecordDTO dto = new AirspaceRecordDTO();
        dto.setRegionName((String) request.get("regionName"));
        dto.setRegionAddress((String) request.get("regionAddress"));
        dto.setLongitude(parseBigDecimal(request.get("longitude"), null));
        dto.setLatitude(parseBigDecimal(request.get("latitude"), null));
        dto.setRadius(parseInt(request.get("radius"), 500));
        dto.setMaxAltitude(parseInt(request.get("maxAltitude"), null));
        dto.setPlannedStartTime(parseDateTime(request.get("plannedStartTime")));
        dto.setPlannedEndTime(parseDateTime(request.get("plannedEndTime")));
        dto.setPurpose((String) request.get("purpose"));

        Long userId = UserContext.getCurrentUserId();
        AirspaceRecord record = airspaceRecordService.submitAirspaceRecord(dto);

        // 通知管理员（Controller 层负责 HTTP/通知层面的横切关注点）
        try {
            String content = "区域：" + record.getRegionName()
                    + "，目的：" + (record.getPurpose() != null && record.getPurpose().length() > 20
                        ? record.getPurpose().substring(0, 20) + "..." : record.getPurpose());
            notificationService.sendNotification(-1L, 4, "空域备案待审核", content, record.getId());

            Map<String, Object> airspaceInfo = new HashMap<>();
            airspaceInfo.put("id", record.getId());
            airspaceInfo.put("userId", userId);
            airspaceInfo.put("regionName", record.getRegionName());
            airspaceInfo.put("content", content);
            airspaceInfo.put("title", "新的空域备案申请");
            notificationHandler.notifyNewAirspace(airspaceInfo);
        } catch (Exception e) {
            log.warn("空域备案通知发送失败（不影响主流程）: {}", e.getMessage());
        }

        return Result.success();
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
