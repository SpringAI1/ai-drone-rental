package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.common.Result;
import com.drone.rental.dto.FaultReportDTO;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.FaultReport;
import com.drone.rental.entity.MaintenanceTicket;
import com.drone.rental.service.DroneService;
import com.drone.rental.service.FaultReportService;
import com.drone.rental.service.MaintenanceTicketService;
import com.drone.rental.service.NotificationService;
import com.drone.rental.service.OrderService;
import com.drone.rental.vo.FaultReportVO;
import com.drone.rental.websocket.OrderNotificationHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 故障上报控制器（用户端）
 */
@Tag(name = "故障上报-用户端")
@RestController
@RequestMapping("/fault")
public class FaultController {

    @Autowired
    private FaultReportService faultReportService;

    @Autowired
    private MaintenanceTicketService maintenanceTicketService;

    @Autowired
    private DroneService droneService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderNotificationHandler notificationHandler;

    @Autowired
    private OrderService orderService;

    @Operation(summary = "上报故障")
    @PostMapping("/report")
    public Result<Void> reportFault(@Validated @RequestBody FaultReportDTO dto) {
        FaultReport report = faultReportService.reportFault(dto);

        // 广播新故障报修通知给管理员
        try {
            String droneModel = "";
            if (dto.getDroneId() != null) {
                Drone drone = droneService.getById(dto.getDroneId());
                if (drone != null) {
                    droneModel = drone.getModel() != null ? drone.getModel() : "";
                }
            }
            String content = "收到新的故障报修：" + (report.getFaultDescription() != null && report.getFaultDescription().length() > 30
                    ? report.getFaultDescription().substring(0, 30) + "..."
                    : report.getFaultDescription());
            notificationService.sendNotification(-1L, 3, droneModel + " 有新的故障报修", content, report.getId());

            Map<String, Object> faultInfo = new HashMap<>();
            faultInfo.put("id", report.getId());
            faultInfo.put("userId", report.getUserId());
            faultInfo.put("description", report.getFaultDescription());
            faultInfo.put("droneModel", droneModel);
            faultInfo.put("title", "新的故障报修");
            notificationHandler.notifyNewFault(faultInfo);
        } catch (Exception e) {
            // 通知失败不影响主流程
        }

        return Result.success();
    }

    @Operation(summary = "获取当前用户的故障上报列表")
    @GetMapping("/my")
    public Result<IPage<FaultReportVO>> getCurrentUserFaultReports(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<FaultReportVO> page = faultReportService.getCurrentUserFaultReportsVO(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据订单ID获取故障报修列表")
    @GetMapping("/order/{orderId}")
    public Result<List<FaultReportVO>> getFaultReportsByOrderId(@PathVariable Long orderId) {
        // 权限校验：orderService.getOrderDetail 内部会校验订单归属（非管理员+非本人→FORBIDDEN）
        orderService.getOrderDetail(orderId);
        List<FaultReportVO> list = faultReportService.getFaultReportsByOrderId(orderId);
        return Result.success(list);
    }

    @Operation(summary = "获取故障上报详情")
    @GetMapping("/{id}")
    public Result<FaultReport> getFaultReportDetail(@PathVariable Long id) {
        FaultReport report = faultReportService.getFaultReportDetail(id);
        return Result.success(report);
    }

    @Operation(summary = "查看维修进度")
    @GetMapping("/{faultReportId}/maintenance")
    public Result<MaintenanceTicket> getMaintenanceProgress(@PathVariable Long faultReportId) {
        MaintenanceTicket ticket = maintenanceTicketService.getByFaultReportId(faultReportId);
        return Result.success(ticket);
    }
}
