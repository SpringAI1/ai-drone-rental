package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.common.Result;
import com.drone.rental.service.OrderService;
import com.drone.rental.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器（管理员端）
 */
@Tag(name = "订单管理-管理员端")
@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "分页查询订单列表")
    @GetMapping("/list")
    public Result<IPage<OrderVO>> pageOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户手机") @RequestParam(required = false) String userPhone,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer orderStatus,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        IPage<OrderVO> result = orderService.pageOrders(page, pageSize, orderNo, userPhone, orderStatus, startDate, endDate);
        return Result.success(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long orderId) {
        OrderVO vo = orderService.getOrderDetail(orderId);
        return Result.success(vo);
    }

    @Operation(summary = "确认归还")
    @PostMapping("/{orderId}/return")
    public Result<Void> confirmReturn(@PathVariable Long orderId) {
        orderService.confirmReturn(orderId);
        return Result.success();
    }

    @Operation(summary = "退款")
    @PostMapping("/{orderId}/refund")
    public Result<Void> refundOrder(
            @PathVariable Long orderId,
            @Parameter(description = "退款原因") @RequestParam(required = false) String reason) {
        orderService.refundOrder(orderId, reason);
        return Result.success();
    }

    @Operation(summary = "发货")
    @PostMapping("/{orderId}/ship")
    public Result<Void> shipOrder(
            @PathVariable Long orderId,
            @Parameter(description = "快递公司") @RequestParam String expressCompany,
            @Parameter(description = "快递单号") @RequestParam String expressNo) {
        orderService.shipOrder(orderId, expressCompany, expressNo);
        return Result.success();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateOrderStatus(
            @PathVariable Long id,
            @Parameter(description = "订单状态") @RequestParam Integer status) {
        orderService.updateOrderStatus(id, status);
        return Result.success();
    }
}
