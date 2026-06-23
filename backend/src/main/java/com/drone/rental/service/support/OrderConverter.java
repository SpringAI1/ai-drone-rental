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

    public OrderVO convertToVO(RentalOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        User user = userService.getById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }

        Drone drone = droneService.getById(order.getDroneId());
        if (drone != null) {
            vo.setDroneModel(drone.getModel());
            vo.setDroneImage(drone.getImage());
        }

        if (order.getAirspaceRecordId() != null) {
            AirspaceRecord record = airspaceRecordService.getById(order.getAirspaceRecordId());
            if (record != null) {
                vo.setRegionName(record.getRegionName());
            }
        }

        Long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getOrderId, order.getId())
                .eq(Comment::getDeleted, 0));
        vo.setHasComment(commentCount > 0);

        return vo;
    }
}
