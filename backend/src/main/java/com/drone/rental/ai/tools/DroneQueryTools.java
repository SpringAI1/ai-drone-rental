package com.drone.rental.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.MaintenanceTicket;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.mapper.DroneMapper;
import com.drone.rental.mapper.MaintenanceTicketMapper;
import com.drone.rental.mapper.RentalOrderMapper;
import com.drone.rental.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业务查询工具 - 使用 Spring AI @Tool 注解
 * 这些方法会被 Spring AI 自动包装为工具，LLM 可在对话中按需调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneQueryTools {

    private final DroneMapper droneMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final MaintenanceTicketMapper maintenanceTicketMapper;

    /** 工具调用时拿当前用户 ID：优先 ToolContext，其次 ThreadLocal */
    private Long currentUserId(ToolContext ctx) {
        if (ctx != null && ctx.getContext() != null && !ctx.getContext().isEmpty()) {
            Object v = ctx.getContext().get("userId");
            if (v instanceof Long) return (Long) v;
            if (v instanceof Number) return ((Number) v).longValue();
        }
        try { return UserContext.getCurrentUserId(); } catch (Exception e) { return null; }
    }

    @Tool(description = """
            查询可租赁的无人机库存。可以按品牌、类型、价格区间、库存数量等条件筛选。
            返回当前在售的无人机列表，包含型号、库存、日租金等关键信息。
            """)
    public String queryAvailableDrones(
            @ToolParam(description = "品牌名，如 DJI / 大疆，可选", required = false) String brand,
            @ToolParam(description = "类型：航拍 / 测绘 / 农业 / 巡检，可选", required = false) String type,
            @ToolParam(description = "最低日租金（元），可选", required = false) Double minPrice,
            @ToolParam(description = "最高日租金（元），可选", required = false) Double maxPrice,
            @ToolParam(description = "最少库存数，可选", required = false) Integer minStock
    ) {
        log.info("[AI Tool] queryAvailableDrones: brand={}, type={}, price=[{},{}], stock>={}",
                brand, type, minPrice, maxPrice, minStock);

        LambdaQueryWrapper<Drone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Drone::getOnShelf, 1).eq(Drone::getStatus, 1);
        if (brand != null && !brand.isBlank()) wrapper.like(Drone::getBrand, brand);
        if (type != null && !type.isBlank()) wrapper.eq(Drone::getType, type);
        if (minPrice != null) wrapper.ge(Drone::getPricePerDay, BigDecimal.valueOf(minPrice));
        if (maxPrice != null) wrapper.le(Drone::getPricePerDay, BigDecimal.valueOf(maxPrice));
        if (minStock != null) wrapper.ge(Drone::getStock, minStock);
        wrapper.orderByDesc(Drone::getCreatedTime).last("LIMIT 10");
        List<Drone> drones = droneMapper.selectList(wrapper);

        if (drones.isEmpty()) {
            return "未找到符合条件的无人机。请放宽筛选条件或提供预算区间让我为您推荐。";
        }

        StringBuilder sb = new StringBuilder("为您找到 ").append(drones.size()).append(" 款可租赁无人机：\n\n");
        for (Drone d : drones) {
            sb.append("【").append(d.getBrand()).append(" ").append(d.getModel()).append("】\n")
              .append("  • 类型：").append(d.getType()).append("\n")
              .append("  • 库存：").append(d.getStock()).append(" 台\n")
              .append("  • 日租金：¥").append(d.getPricePerDay()).append("/天\n")
              .append("  • 续航：").append(d.getFlightTime()).append(" 分钟\n")
              .append("  • 最大航程：").append(d.getMaxRange()).append(" km\n")
              .append("  • 简介：").append(d.getDescription() != null ? d.getDescription() : "（暂无）").append("\n")
              .append("  • 无人机ID：").append(d.getId()).append("\n\n");
        }
        return sb.toString();
    }

    @Tool(description = "根据无人机ID查询其详细信息，包括价格、配置参数、库存等")
    public String queryDroneDetail(@ToolParam(description = "无人机ID，必填") Long droneId) {
        log.info("[AI Tool] queryDroneDetail: droneId={}", droneId);
        if (droneId == null) return "错误：缺少无人机ID参数";
        Drone d = droneMapper.selectById(droneId);
        if (d == null) return "未找到ID为 " + droneId + " 的无人机";

        return String.format("""
                【%s %s 详细信息】
                • 品牌：%s
                • 类型：%s
                • 日租金：¥%s/天
                • 库存：%d 台
                • 续航时间：%d 分钟
                • 最大载重：%s kg
                • 最大速度：%s km/h
                • 最大航程：%s km
                • 状态：%s
                • 简介：%s
                """,
                d.getBrand(), d.getModel(),
                d.getBrand(), d.getType(),
                d.getPricePerDay(), d.getStock(),
                d.getFlightTime(), d.getMaxPayload(), d.getMaxSpeed(), d.getMaxRange(),
                d.getStatus() == 1 ? "在售" : "缺货/维护",
                d.getDescription() != null ? d.getDescription() : "（暂无）");
    }

    @Tool(description = "根据场景推荐合适的无人机：旅行拍摄、专业航拍、商业拍摄、农业巡检、入门等")
    public String recommendDroneByScenario(
            @ToolParam(description = "使用场景：旅行/日常/专业/商业/农业/巡检/入门") String scenario
    ) {
        log.info("[AI Tool] recommendDroneByScenario: scenario={}", scenario);
        if (scenario == null) return "请告诉我您的使用场景（如：旅行拍摄、专业航拍、商业拍摄、农业巡检）";

        LambdaQueryWrapper<Drone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Drone::getOnShelf, 1).eq(Drone::getStatus, 1);

        String hint = scenario;
        // 按场景匹配
        switch (scenario) {
            case "旅行", "日常", "入门" -> wrapper.and(w -> w.like(Drone::getType, "入门")
                    .or().like(Drone::getModel, "Mini").or().like(Drone::getModel, "入门"))
                    .orderByAsc(Drone::getPricePerDay);
            case "专业", "航拍" -> wrapper.and(w -> w.like(Drone::getType, "专业")
                    .or().like(Drone::getType, "航拍").or().like(Drone::getModel, "Air"))
                    .orderByAsc(Drone::getPricePerDay);
            case "商业", "旗舰" -> wrapper.and(w -> w.like(Drone::getType, "商业")
                    .or().like(Drone::getModel, "Mavic").or().like(Drone::getModel, "Inspire"))
                    .orderByAsc(Drone::getPricePerDay);
            case "农业", "巡检" -> wrapper.and(w -> w.like(Drone::getType, "农业")
                    .or().like(Drone::getType, "巡检").or().like(Drone::getModel, "Agras")
                    .or().like(Drone::getModel, "Phantom"));
            default -> wrapper.orderByDesc(Drone::getCreatedTime);
        }
        wrapper.last("LIMIT 5");
        List<Drone> drones = droneMapper.selectList(wrapper);
        if (drones.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Drone::getOnShelf, 1).eq(Drone::getStatus, 1)
                    .orderByAsc(Drone::getPricePerDay).last("LIMIT 3");
            drones = droneMapper.selectList(wrapper);
            hint = "没有完全匹配「" + scenario + "」的机型，但以下几款也很受欢迎";
        }

        StringBuilder sb = new StringBuilder("根据您的「").append(scenario).append("」场景，").append(hint).append("：\n\n");
        for (Drone d : drones) {
            sb.append("【").append(d.getBrand()).append(" ").append(d.getModel()).append("】")
              .append("  ¥").append(d.getPricePerDay()).append("/天  |  库存 ").append(d.getStock()).append(" 台  |  续航 ")
              .append(d.getFlightTime()).append("分钟\n")
              .append("    ").append(d.getDescription() != null ? d.getDescription() : "（暂无）").append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "查询某用户的所有订单记录。管理员可指定 userId；普通用户只能查自己的")
    public String queryUserOrders(
            @ToolParam(description = "用户ID（管理员可指定；不填则查当前登录用户）", required = false) Long userId,
            @ToolParam(description = "订单状态：0-待支付 1-已支付 2-租赁中 3-已归还 4-已取消 5-已退款，可选", required = false) Integer orderStatus,
            @ToolParam(description = "返回条数，默认10", required = false) Integer limit,
            ToolContext toolContext
    ) {
        log.info("[AI Tool] queryUserOrders: userId={}, orderStatus={}, limit={}", userId, orderStatus, limit);
        Long target = userId;
        if (target == null) {
            target = currentUserId(toolContext);
        }
        if (target == null) return "请先登录后再查询订单";

        int n = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getUserId, target);
        if (orderStatus != null) wrapper.eq(RentalOrder::getOrderStatus, orderStatus);
        wrapper.orderByDesc(RentalOrder::getCreatedTime).last("LIMIT " + n);

        List<RentalOrder> orders = rentalOrderMapper.selectList(wrapper);
        if (orders.isEmpty()) return "暂无订单记录";

        StringBuilder sb = new StringBuilder("用户 ").append(target).append(" 的订单（共 ").append(orders.size()).append(" 条）：\n\n");
        for (RentalOrder o : orders) {
            sb.append("• 订单号：").append(o.getOrderNo())
              .append("  |  状态：").append(statusOf(o.getOrderStatus()))
              .append("  |  金额：¥").append(o.getTotalAmount())
              .append("  |  起止：").append(o.getRentalStartTime()).append(" ~ ").append(o.getRentalEndTime()).append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "根据订单号查询订单详细状态")
    public String queryOrderStatus(@ToolParam(description = "订单编号") String orderNo) {
        log.info("[AI Tool] queryOrderStatus: orderNo={}", orderNo);
        if (orderNo == null) return "错误：缺少订单编号";
        RentalOrder order = rentalOrderMapper.selectOne(
                new LambdaQueryWrapper<RentalOrder>().eq(RentalOrder::getOrderNo, orderNo));
        if (order == null) return "未找到订单 " + orderNo;

        return String.format("""
                订单 %s 详细信息：
                • 状态：%s
                • 租赁起止：%s ~ %s（%d 天）
                • 总金额：¥%s（押金 ¥%s）
                • 收货地址：%s
                • 下单时间：%s
                %s
                """,
                order.getOrderNo(), statusOf(order.getOrderStatus()),
                order.getRentalStartTime(), order.getRentalEndTime(), order.getRentalDays(),
                order.getTotalAmount(), order.getDepositAmount(),
                order.getDeliveryAddress(), order.getCreatedTime(),
                order.getCancelReason() != null ? "• 取消原因：" + order.getCancelReason() : "");
    }

    @Tool(description = "查询无人机的维修工单记录")
    public String queryMaintenanceRecords(
            @ToolParam(description = "无人机ID，可选", required = false) Long droneId,
            @ToolParam(description = "工单编号，可选", required = false) String ticketNo,
            @ToolParam(description = "工单状态：0-待维修 1-维修中 2-已完成 3-已取消，可选", required = false) Integer status
    ) {
        log.info("[AI Tool] queryMaintenanceRecords: droneId={}, ticketNo={}, status={}", droneId, ticketNo, status);
        LambdaQueryWrapper<MaintenanceTicket> wrapper = new LambdaQueryWrapper<>();
        if (droneId != null) wrapper.eq(MaintenanceTicket::getDroneId, droneId);
        if (ticketNo != null) wrapper.eq(MaintenanceTicket::getTicketNo, ticketNo);
        if (status != null) wrapper.eq(MaintenanceTicket::getStatus, status);
        wrapper.orderByDesc(MaintenanceTicket::getCreatedTime).last("LIMIT 10");

        List<MaintenanceTicket> tickets = maintenanceTicketMapper.selectList(wrapper);
        if (tickets.isEmpty()) return "未找到符合条件的维修记录";

        StringBuilder sb = new StringBuilder("维修记录（共 ").append(tickets.size()).append(" 条）：\n\n");
        for (MaintenanceTicket t : tickets) {
            sb.append("• 工单号：").append(t.getTicketNo())
              .append("  |  无人机ID：").append(t.getDroneId())
              .append("  |  类型：").append(t.getMaintenanceType())
              .append("  |  状态：").append(maintenanceStatusOf(t.getStatus()))
              .append("  |  费用：¥").append(t.getActualCost() != null ? t.getActualCost() : "0").append("\n");
        }
        return sb.toString();
    }

    private static String statusOf(Integer s) {
        if (s == null) return "未知";
        return switch (s) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "租赁中";
            case 3 -> "已归还";
            case 4 -> "已取消";
            case 5 -> "已退款";
            default -> "未知";
        };
    }

    private static String maintenanceStatusOf(Integer s) {
        if (s == null) return "未知";
        return switch (s) {
            case 0 -> "待维修";
            case 1 -> "维修中";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }
}
