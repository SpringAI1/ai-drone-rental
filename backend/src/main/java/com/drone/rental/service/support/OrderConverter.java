package com.drone.rental.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.entity.AirspaceRecord;
import com.drone.rental.entity.Comment;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.entity.User;
import com.drone.rental.mapper.CommentMapper;
import com.drone.rental.service.AirspaceRecordService;
import com.drone.rental.service.DroneService;
import com.drone.rental.service.UserService;
import com.drone.rental.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单 VO 转换工具（从 OrderServiceImpl 抽出以降低类大小）
 */
@Component
public class OrderConverter {

    private final UserService userService;
    private final DroneService droneService;
    private final AirspaceRecordService airspaceRecordService;
    private final CommentMapper commentMapper;

    public OrderConverter(UserService userService,
                          DroneService droneService,
                          AirspaceRecordService airspaceRecordService,
                          CommentMapper commentMapper) {
        this.userService = userService;
        this.droneService = droneService;
        this.airspaceRecordService = airspaceRecordService;
        this.commentMapper = commentMapper;
    }

    /** 单条转换（兼容旧调用，如 getOrderDetail） */
    public OrderVO convertToVO(RentalOrder order) {
        return convertToVO(order, null, null, null, null);
    }

    /**
     * 批量转换，预加载关联数据，避免 N+1 查询
     */
    public List<OrderVO> convertBatch(List<RentalOrder> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();

        // 收集 IDs
        Set<Long> userIds = new HashSet<>();
        Set<Long> droneIds = new HashSet<>();
        Set<Long> airspaceIds = new HashSet<>();
        for (RentalOrder o : orders) {
            if (o.getUserId() != null) userIds.add(o.getUserId());
            if (o.getDroneId() != null) droneIds.add(o.getDroneId());
            if (o.getAirspaceRecordId() != null) airspaceIds.add(o.getAirspaceRecordId());
        }

        // 批量加载
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Drone> droneMap = droneIds.isEmpty() ? Collections.emptyMap() :
                droneService.listByIds(droneIds).stream().collect(Collectors.toMap(Drone::getId, Function.identity()));
        Map<Long, AirspaceRecord> airspaceMap = airspaceIds.isEmpty() ? Collections.emptyMap() :
                airspaceRecordService.listByIds(airspaceIds).stream().collect(Collectors.toMap(AirspaceRecord::getId, Function.identity()));

        // 批量查询评论计数（一次 IN 查询）
        List<Long> orderIds = orders.stream().map(RentalOrder::getId).collect(Collectors.toList());
        Map<Long, Long> commentCountMap = orderIds.isEmpty() ? Collections.emptyMap() :
                commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .in(Comment::getOrderId, orderIds)
                        .eq(Comment::getDeleted, 0))
                        .stream()
                        .collect(Collectors.groupingBy(Comment::getOrderId, Collectors.counting()));

        return orders.stream()
                .map(o -> convertToVO(o, userMap, droneMap, airspaceMap, commentCountMap))
                .collect(Collectors.toList());
    }

    private OrderVO convertToVO(RentalOrder order, Map<Long, User> userMap, Map<Long, Drone> droneMap,
                                 Map<Long, AirspaceRecord> airspaceMap, Map<Long, Long> commentCountMap) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        if (userMap != null && order.getUserId() != null) {
            User user = userMap.get(order.getUserId());
            if (user != null) {
                vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        } else if (order.getUserId() != null) {
            User user = userService.getById(order.getUserId());
            if (user != null) vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }

        if (droneMap != null && order.getDroneId() != null) {
            Drone drone = droneMap.get(order.getDroneId());
            if (drone != null) {
                vo.setDroneModel(drone.getModel());
                vo.setDroneImage(drone.getImage());
            }
        } else if (order.getDroneId() != null) {
            Drone drone = droneService.getById(order.getDroneId());
            if (drone != null) {
                vo.setDroneModel(drone.getModel());
                vo.setDroneImage(drone.getImage());
            }
        }

        if (airspaceMap != null && order.getAirspaceRecordId() != null) {
            AirspaceRecord record = airspaceMap.get(order.getAirspaceRecordId());
            if (record != null) vo.setRegionName(record.getRegionName());
        } else if (order.getAirspaceRecordId() != null) {
            AirspaceRecord record = airspaceRecordService.getById(order.getAirspaceRecordId());
            if (record != null) vo.setRegionName(record.getRegionName());
        }

        if (commentCountMap != null) {
            Long count = commentCountMap.getOrDefault(order.getId(), 0L);
            vo.setHasComment(count > 0);
        } else {
            Long count = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getOrderId, order.getId())
                    .eq(Comment::getDeleted, 0));
            vo.setHasComment(count > 0);
        }

        return vo;
    }
}
