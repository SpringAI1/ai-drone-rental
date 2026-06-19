package com.drone.rental.ai.tools;

import com.drone.rental.entity.Drone;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.service.DroneService;
import com.drone.rental.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiTools {

    private final DroneService droneService;
    private final OrderService orderService;

    public String getDroneList() {
        log.info("调用无人机列表工具");
        List<Drone> drones = droneService.listAll();
        StringBuilder sb = new StringBuilder("当前可租赁的无人机列表：\n");
        for (Drone d : drones) {
            sb.append(String.format(
                "- %s (%s): ¥%.0f/天, 库存%d架, 续航%d分钟\n",
                d.getModel(), d.getType(), d.getPricePerDay(), d.getStock(), d.getFlightTime()
            ));
        }
        return sb.toString();
    }

    public String getDroneDetail(String model) {
        log.info("调用无人机详情工具，型号: {}", model);
        Drone drone = droneService.getByModel(model);
        if (drone == null) {
            return "未找到型号为 " + model + " 的无人机";
        }
        return String.format("""
            无人机详情：
            - 型号：%s
            - 品牌：%s
            - 类型：%s
            - 描述：%s
            - 日租金：¥%.0f
            - 库存：%d架
            - 续航：%d分钟
            - 最大载荷：%.1fkg
            - 最大速度：%.0fkm/h
            - 最大距离：%.0fkm
            """,
            drone.getModel(), drone.getBrand(), drone.getType(),
            drone.getDescription(), drone.getPricePerDay(),
            drone.getStock(), drone.getFlightTime(),
            drone.getMaxPayload(), drone.getMaxSpeed(), drone.getMaxRange()
        );
    }

    public String getOrderStatus(Long orderId) {
        log.info("调用订单状态工具，订单号: {}", orderId);
        RentalOrder order = orderService.getById(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        String statusText = switch (order.getOrderStatus()) {
            case 0 -> "待支付";
            case 1 -> "已支付待取货";
            case 2 -> "使用中";
            case 3 -> "已归还待验收";
            case 4 -> "已完成";
            case 5 -> "已取消";
            default -> "未知状态";
        };
        Drone drone = droneService.getById(order.getDroneId());
        return String.format("""
            订单信息：
            - 订单号：%d
            - 状态：%s
            - 无人机：%s
            - 租赁天数：%d天
            - 总金额：¥%.0f
            - 开始时间：%s
            - 结束时间：%s
            """,
            order.getId(), statusText,
            drone != null ? drone.getModel() : "未知",
            order.getRentalDays(), order.getTotalAmount(),
            order.getRentalStartTime(), order.getRentalEndTime()
        );
    }
}