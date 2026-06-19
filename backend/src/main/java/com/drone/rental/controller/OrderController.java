package com.drone.rental.controller;

import com.drone.rental.common.Result;
import com.drone.rental.dto.OrderCreateDTO;
import com.drone.rental.service.OrderService;
import com.drone.rental.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器（用户端）
 */
@Tag(name = "订单管理-用户端")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(@Validated @RequestBody OrderCreateDTO dto) {
        OrderVO vo = orderService.createOrder(dto);
        return Result.success(vo);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long orderId) {
        OrderVO vo = orderService.getOrderDetail(orderId);
        return Result.success(vo);
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/{orderId}/pay")
    public Result<Void> simulatePay(@PathVariable Long orderId, @RequestBody(required = false) java.util.Map<String, Object> params) {
        String deliveryAddress = params != null ? (String) params.get("deliveryAddress") : null;
        Integer paymentMethod = params != null ? (Integer) params.get("paymentMethod") : null;
        orderService.simulatePay(orderId, deliveryAddress, paymentMethod);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) java.util.Map<String, Object> params) {
        String reason = params != null ? (String) params.get("reason") : null;
        orderService.cancelOrder(orderId, reason);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderId}/receive")
    public Result<Void> confirmReceive(@PathVariable Long orderId) {
        orderService.confirmReceive(orderId);
        return Result.success();
    }

    @Operation(summary = "申请退租")
    @PostMapping("/{orderId}/return")
    public Result<Void> applyReturn(@PathVariable Long orderId) {
        orderService.applyReturn(orderId);
        return Result.success();
    }

    @Operation(summary = "申请退款")
    @PostMapping("/{orderId}/refund")
    public Result<Void> applyRefund(
            @PathVariable Long orderId,
            @RequestBody(required = false) java.util.Map<String, Object> params) {
        String reason = params != null ? (String) params.get("reason") : null;
        orderService.applyRefund(orderId, reason);
        return Result.success();
    }

    @Operation(summary = "发起微信支付")
    @PostMapping("/{orderId}/pay/wechat")
    public Result<java.util.Map<String, Object>> payByWechat(@PathVariable Long orderId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("payType", "wechat");
        result.put("orderId", orderId);
        result.put("orderNo", "MOCK_WX_" + System.currentTimeMillis());
        result.put("paymentMethod", 2);
        result.put("status", "success");
        result.put("message", "模拟微信支付已发起，请在模拟环境支付完成后调用 /confirm 完成实际支付");
        result.put("timestamp", System.currentTimeMillis());
        return Result.success(result);
    }

    @Operation(summary = "发起支付宝支付")
    @PostMapping("/{orderId}/pay/alipay")
    public Result<java.util.Map<String, Object>> payByAlipay(@PathVariable Long orderId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("payType", "alipay");
        result.put("orderId", orderId);
        result.put("orderNo", "MOCK_ALIPAY_" + System.currentTimeMillis());
        result.put("paymentMethod", 3);
        result.put("status", "success");
        result.put("message", "模拟支付宝支付已发起，请在模拟环境支付完成后调用 /confirm 完成实际支付");
        result.put("timestamp", System.currentTimeMillis());
        return Result.success(result);
    }

    @Operation(summary = "查询支付是否完成")
    @PostMapping("/{orderId}/pay/status")
    public Result<java.util.Map<String, Object>> payStatus(@PathVariable Long orderId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("orderId", orderId);
        result.put("status", "SUCCESS");
        result.put("paymentTime", java.time.LocalDateTime.now().toString());
        return Result.success(result);
    }

    @Operation(summary = "查询是否允许退款")
    @PostMapping("/{orderId}/refund/status")
    public Result<java.util.Map<String, Object>> refundStatus(@PathVariable Long orderId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("orderId", orderId);
        result.put("status", "SUCCESS");
        result.put("refundTime", java.time.LocalDateTime.now().toString());
        return Result.success(result);
    }
}
