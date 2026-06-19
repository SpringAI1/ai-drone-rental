package com.drone.rental.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.MaintenanceTicket;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.mapper.DroneMapper;
import com.drone.rental.mapper.MaintenanceTicketMapper;
import com.drone.rental.mapper.RentalOrderMapper;
import com.drone.rental.vo.OrderVO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ToolExecutor {

    private final DroneMapper droneMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final MaintenanceTicketMapper maintenanceTicketMapper;

    public ToolExecutor(DroneMapper droneMapper, RentalOrderMapper rentalOrderMapper, MaintenanceTicketMapper maintenanceTicketMapper) {
        this.droneMapper = droneMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.maintenanceTicketMapper = maintenanceTicketMapper;
    }

    public String execute(String toolName, Map<String, Object> arguments) {
        switch (toolName) {
            case "query_drone_stock":
                return queryDroneStock(arguments);
            case "query_drone_detail":
                return queryDroneDetail(arguments);
            case "query_order_status":
                return queryOrderStatus(arguments);
            case "query_user_orders":
                return queryUserOrders(arguments);
            case "query_maintenance_records":
                return queryMaintenanceRecords(arguments);
            default:
                return "未知工具: " + toolName;
        }
    }

    private String queryDroneStock(Map<String, Object> arguments) {
        LambdaQueryWrapper<Drone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Drone::getOnShelf, 1);

        if (arguments.containsKey("brand") && arguments.get("brand") != null) {
            wrapper.eq(Drone::getBrand, arguments.get("brand").toString());
        }
        if (arguments.containsKey("type") && arguments.get("type") != null) {
            wrapper.eq(Drone::getType, arguments.get("type").toString());
        }
        if (arguments.containsKey("min_stock")) {
            wrapper.ge(Drone::getStock, ((Number) arguments.get("min_stock")).intValue());
        }

        wrapper.orderByDesc(Drone::getCreatedTime);
        List<Drone> drones = droneMapper.selectList(wrapper);

        if (drones.isEmpty()) {
            return "未找到符合条件的无人机库存信息";
        }

        StringBuilder result = new StringBuilder("无人机库存列表（共" + drones.size() + "条）：\n\n");
        for (Drone drone : drones) {
            result.append("【").append(drone.getBrand()).append(" ").append(drone.getModel()).append("】\n");
            result.append("  类型：").append(drone.getType()).append("\n");
            result.append("  库存：").append(drone.getStock()).append("台\n");
            result.append("  日租金：¥").append(drone.getPricePerDay()).append("/天\n");
            result.append("  状态：").append(getStockStatus(drone.getStatus())).append("\n");
            result.append("  ID：").append(drone.getId()).append("\n\n");
        }

        return result.toString();
    }

    private String queryDroneDetail(Map<String, Object> arguments) {
        if (!arguments.containsKey("drone_id")) {
            return "错误：缺少无人机ID参数";
        }

        Long droneId = ((Number) arguments.get("drone_id")).longValue();
        Drone drone = droneMapper.selectById(droneId);

        if (drone == null) {
            return "未找到ID为" + droneId + "的无人机";
        }

        StringBuilder result = new StringBuilder();
        result.append("【无人机详细信息】\n\n");
        result.append("型号：").append(drone.getModel()).append("\n");
        result.append("品牌：").append(drone.getBrand()).append("\n");
        result.append("类型：").append(drone.getType()).append("\n");
        result.append("描述：").append(drone.getDescription()).append("\n");
        result.append("日租金：¥").append(drone.getPricePerDay()).append("/天\n");
        result.append("库存：").append(drone.getStock()).append("台\n");
        result.append("续航时间：").append(drone.getFlightTime()).append("分钟\n");
        result.append("最大载重：").append(drone.getMaxPayload()).append("kg\n");
        result.append("最大速度：").append(drone.getMaxSpeed()).append("km/h\n");
        result.append("最大航程：").append(drone.getMaxRange()).append("km\n");
        result.append("状态：").append(getStockStatus(drone.getStatus())).append("\n");
        result.append("上架状态：").append(drone.getOnShelf() == 1 ? "上架中" : "已下架").append("\n");

        return result.toString();
    }

    private String queryOrderStatus(Map<String, Object> arguments) {
        if (!arguments.containsKey("order_no")) {
            return "错误：缺少订单编号参数";
        }

        String orderNo = arguments.get("order_no").toString();
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getOrderNo, orderNo);
        RentalOrder order = rentalOrderMapper.selectOne(wrapper);

        if (order == null) {
            return "未找到订单编号为" + orderNo + "的订单";
        }

        StringBuilder result = new StringBuilder();
        result.append("【订单信息】\n\n");
        result.append("订单编号：").append(order.getOrderNo()).append("\n");
        result.append("订单状态：").append(getOrderStatus(order.getOrderStatus())).append("\n");
        result.append("租赁开始：").append(order.getRentalStartTime()).append("\n");
        result.append("租赁结束：").append(order.getRentalEndTime()).append("\n");
        result.append("租赁天数：").append(order.getRentalDays()).append("天\n");
        result.append("订单金额：¥").append(order.getTotalAmount()).append("\n");
        result.append("押金：¥").append(order.getDepositAmount()).append("\n");
        result.append("收货地址：").append(order.getDeliveryAddress()).append("\n");

        if (order.getCancelReason() != null && !order.getCancelReason().isEmpty()) {
            result.append("取消原因：").append(order.getCancelReason()).append("\n");
        }

        return result.toString();
    }

    private String queryUserOrders(Map<String, Object> arguments) {
        Long userId = null;
        Integer orderStatus = null;
        int limit = 10;

        if (arguments.containsKey("user_id") && arguments.get("user_id") != null) {
            userId = ((Number) arguments.get("user_id")).longValue();
        }
        if (arguments.containsKey("order_status") && arguments.get("order_status") != null) {
            orderStatus = ((Number) arguments.get("order_status")).intValue();
        }
        if (arguments.containsKey("limit") && arguments.get("limit") != null) {
            limit = Math.min(((Number) arguments.get("limit")).intValue(), 50);
        }

        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(RentalOrder::getUserId, userId);
        }
        if (orderStatus != null) {
            wrapper.eq(RentalOrder::getOrderStatus, orderStatus);
        }
        wrapper.orderByDesc(RentalOrder::getCreatedTime);
        wrapper.last("LIMIT " + limit);

        List<RentalOrder> orders = rentalOrderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return "未找到符合条件的订单记录";
        }

        StringBuilder result = new StringBuilder();
        result.append("【用户订单列表】（共").append(orders.size()).append("条）：\n\n");

        for (RentalOrder order : orders) {
            result.append("订单号：").append(order.getOrderNo());
            result.append(" | 状态：").append(getOrderStatus(order.getOrderStatus()));
            result.append(" | 金额：¥").append(order.getTotalAmount());
            result.append(" | 日期：").append(order.getCreatedTime()).append("\n");
        }

        return result.toString();
    }

    private String queryMaintenanceRecords(Map<String, Object> arguments) {
        Long droneId = null;
        String ticketNo = null;
        Integer status = null;
        int limit = 10;

        if (arguments.containsKey("drone_id") && arguments.get("drone_id") != null) {
            droneId = ((Number) arguments.get("drone_id")).longValue();
        }
        if (arguments.containsKey("ticket_no") && arguments.get("ticket_no") != null) {
            ticketNo = arguments.get("ticket_no").toString();
        }
        if (arguments.containsKey("status") && arguments.get("status") != null) {
            status = ((Number) arguments.get("status")).intValue();
        }
        if (arguments.containsKey("limit") && arguments.get("limit") != null) {
            limit = Math.min(((Number) arguments.get("limit")).intValue(), 50);
        }

        LambdaQueryWrapper<MaintenanceTicket> wrapper = new LambdaQueryWrapper<>();
        if (droneId != null) {
            wrapper.eq(MaintenanceTicket::getDroneId, droneId);
        }
        if (ticketNo != null && !ticketNo.isEmpty()) {
            wrapper.eq(MaintenanceTicket::getTicketNo, ticketNo);
        }
        if (status != null) {
            wrapper.eq(MaintenanceTicket::getStatus, status);
        }
        wrapper.orderByDesc(MaintenanceTicket::getCreatedTime);
        wrapper.last("LIMIT " + limit);

        List<MaintenanceTicket> tickets = maintenanceTicketMapper.selectList(wrapper);

        if (tickets.isEmpty()) {
            return "未找到符合条件的维修记录";
        }

        StringBuilder result = new StringBuilder();
        result.append("【维修记录列表】（共").append(tickets.size()).append("条）：\n\n");

        for (MaintenanceTicket ticket : tickets) {
            result.append("工单号：").append(ticket.getTicketNo());
            result.append(" | 无人机ID：").append(ticket.getDroneId());
            result.append(" | 类型：").append(ticket.getMaintenanceType());
            result.append(" | 状态：").append(getMaintenanceStatus(ticket.getStatus()));
            result.append(" | 费用：¥").append(ticket.getActualCost()).append("\n");
        }

        return result.toString();
    }

    private String getStockStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "缺货";
            case 1: return "在售";
            case 2: return "维护中";
            default: return "未知";
        }
    }

    private String getOrderStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "租赁中";
            case 3: return "已归还";
            case 4: return "已取消";
            case 5: return "已退款";
            default: return "未知";
        }
    }

    private String getMaintenanceStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待维修";
            case 1: return "维修中";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }
}