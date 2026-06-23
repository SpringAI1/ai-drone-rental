package com.drone.rental.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.dto.OrderCreateDTO;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.entity.UserQualification;
import com.drone.rental.mapper.DroneMapper;
import com.drone.rental.mapper.RentalOrderMapper;
import com.drone.rental.mapper.UserQualificationMapper;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.OrderService;
import com.drone.rental.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 业务操作工具 - 智能下单、查询资质等
 * 这些方法会被 LLM 在对话中按需调用（Function Calling）
 *
 * 注意：
 * 1) Spring AI 的工具调用可能在异步线程池中执行，导致 UserContext (ThreadLocal) 失效
 * 2) 改用 ToolContext 透传 userId / username（由 DroneChatService 在 .toolContext() 注入）
 * 3) 兜底再尝试 ThreadLocal，方便直接调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainOperationTools {

    private final DroneMapper droneMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final UserQualificationMapper userQualificationMapper;
    private final OrderService orderService;

    /** 工具调用时拿当前用户 ID：优先 ToolContext，其次 ThreadLocal */
    private Long currentUserId(ToolContext ctx) {
        if (ctx != null && ctx.getContext() != null && !ctx.getContext().isEmpty()) {
            Object v = ctx.getContext().get("userId");
            if (v instanceof Long) return (Long) v;
            if (v instanceof Number) return ((Number) v).longValue();
        }
        try { return UserContext.getCurrentUserId(); } catch (Exception e) { return null; }
    }

    private String currentUsername(ToolContext ctx) {
        if (ctx != null && ctx.getContext() != null) {
            Object v = ctx.getContext().get("username");
            if (v != null) return String.valueOf(v);
        }
        try { return UserContext.getCurrentUsername(); } catch (Exception e) { return null; }
    }

    @Tool(description = """
            智能下单：根据用户的需求（无人机型号或品牌 + 起止日期 + 收货地址）创建租赁订单。
            会自动检查：1) 资质审核状态 2) 库存可用性 3) 日期合法性 4) 当前用户登录态。
            若用户未登录或资质未通过，会返回明确提示，不会强行下单。
            """)
    public String smartCreateOrder(
            @ToolParam(description = "无人机ID（如果用户提到具体机型可先 queryDroneDetail 拿到 ID）", required = false) Long droneId,
            @ToolParam(description = "起租日期，格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd", required = false) String endDate,
            @ToolParam(description = "收货地址", required = false) String deliveryAddress,
            @ToolParam(description = "订单备注，可选", required = false) String remark,
            ToolContext toolContext
    ) {
        log.info("[AI Tool] smartCreateOrder: droneId={}, {}~{}, addr={}",
                droneId, startDate, endDate, deliveryAddress);

        // 0) 登录态
        Long userId = currentUserId(toolContext);
        if (userId == null) return "下单失败：当前会话未登录或登录已过期，请重新登录后再试";

        // 1) 资质审核
        UserQualification q = userQualificationMapper.selectOne(
                new LambdaQueryWrapper<UserQualification>()
                        .eq(UserQualification::getUserId, userId)
                        .orderByDesc(UserQualification::getCreatedTime).last("LIMIT 1"));
        if (q == null) return "下单失败：您尚未提交飞行资质审核，请先在「我的 - 飞行资质」中提交证书。";
        if (q.getAuditStatus() == null || q.getAuditStatus() != 1) {
            return "下单失败：您的飞行资质尚未通过审核（当前状态："
                    + (q.getAuditStatus() == null ? "未提交" : q.getAuditStatus() == 0 ? "待审核" : "已拒绝")
                    + "）。请联系管理员审核后再下单。";
        }

        // 2) 无人机
        if (droneId == null) return "下单失败：未指定无人机ID，请先 queryAvailableDrones 选定机型。";
        Drone drone = droneMapper.selectById(droneId);
        if (drone == null) return "下单失败：未找到无人机 ID=" + droneId;
        if (drone.getOnShelf() == null || drone.getOnShelf() != 1) return "下单失败：该机型已下架";
        if (drone.getStock() == null || drone.getStock() <= 0) return "下单失败：该机型暂无库存";

        // 3) 日期
        LocalDate start, end;
        try {
            start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
            end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return "下单失败：日期格式错误，请使用 yyyy-MM-dd 格式（如 2026-08-10）";
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) return "下单失败：结束日期必须晚于起租日期";
        if (days > 90) return "下单失败：单次租赁最长 90 天";
        if (start.isBefore(LocalDate.now())) return "下单失败：起租日期不能早于今天";

        // 4) 地址
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            return "下单失败：收货地址不能为空";
        }

        // 5) 创建订单
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setDroneId(droneId);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setDeliveryAddress(deliveryAddress);
        dto.setRemark(remark != null ? remark : "AI 智能下单");
        try {
            OrderVO vo = orderService.createOrder(dto);
            return String.format("""
                    下单成功！
                    • 订单号：%s
                    • 无人机：%s %s
                    • 租赁：%s ~ %s（%d 天）
                    • 总金额：¥%s（押金 ¥%s）
                    • 收货地址：%s
                    • 下一步：可调用 simulatePayOrder 直接完成支付
                    """,
                    vo.getOrderNo(), drone.getBrand(), drone.getModel(),
                    startDate, endDate, days,
                    vo.getTotalAmount(), vo.getDepositAmount(),
                    deliveryAddress);
        } catch (Exception e) {
            log.warn("[AI Tool] 下单失败", e);
            return "下单失败：" + e.getMessage();
        }
    }

    @Tool(description = "模拟支付订单（用于AI助手代用户完成支付）")
    public String simulatePayOrder(
            @ToolParam(description = "订单号") String orderNo,
            @ToolParam(description = "支付方式：1-微信 2-支付宝 3-余额，可选默认 3", required = false) Integer paymentMethod,
            ToolContext toolContext
    ) {
        log.info("[AI Tool] simulatePayOrder: orderNo={}, method={}", orderNo, paymentMethod);
        if (orderNo == null || orderNo.isBlank()) return "错误：缺少订单号";
        RentalOrder order = rentalOrderMapper.selectOne(
                new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getOrderNo, orderNo));
        if (order == null) return "未找到订单 " + orderNo;
        if (order.getOrderStatus() != 0) return "订单 " + orderNo + " 当前状态不是「待支付」，无法支付";

        // 安全校验：只能支付自己的订单（除非管理员）
        Long userId = currentUserId(toolContext);
        Integer role = null;
        try { role = UserContext.getCurrentRole(); } catch (Exception ignored) {}
        if (userId != null && role != null && role == 0 && !userId.equals(order.getUserId())) {
            return "支付失败：订单 " + orderNo + " 不属于您，无权操作";
        }

        try {
            int method = (paymentMethod == null) ? 3 : paymentMethod;
            orderService.simulatePay(order.getId(), order.getDeliveryAddress(), method);
            return "支付成功！订单 " + orderNo + " 已支付，等待管理员发货。";
        } catch (Exception e) {
            return "支付失败：" + e.getMessage();
        }
    }

    @Tool(description = "查询当前用户的资质审核状态（是否已通过、证书编号、有效期）")
    public String queryMyQualification(ToolContext toolContext) {
        Long userId = currentUserId(toolContext);
        if (userId == null) return "请先登录";

        List<UserQualification> list = userQualificationMapper.selectList(
                new LambdaQueryWrapper<UserQualification>()
                        .eq(UserQualification::getUserId, userId)
                        .orderByDesc(UserQualification::getCreatedTime));
        if (list.isEmpty()) return "您尚未提交飞行资质审核，请前往「我的 - 飞行资质」提交证书。";

        UserQualification q = list.get(0);
        String status = switch (q.getAuditStatus() == null ? -1 : q.getAuditStatus()) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
        return String.format("""
                您的飞行资质信息：
                • 证书编号：%s
                • 证书类型：%s
                • 有效期：%s ~ %s
                • 审核状态：%s
                %s
                """,
                q.getCertificateNo(), q.getCertificateType(),
                q.getValidStartDate(), q.getValidEndDate(), status,
                q.getAuditRemark() != null ? "• 审核备注：" + q.getAuditRemark() : "");
    }
}
