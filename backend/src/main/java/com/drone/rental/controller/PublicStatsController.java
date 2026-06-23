package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.common.Result;
import com.drone.rental.entity.Comment;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.entity.User;
import com.drone.rental.mapper.CommentMapper;
import com.drone.rental.mapper.DroneMapper;
import com.drone.rental.mapper.RentalOrderMapper;
import com.drone.rental.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "公共统计")
@RestController
@RequestMapping("/public")
public class PublicStatsController {

    @Autowired
    @Lazy
    private PublicStatsController self;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DroneMapper droneMapper;

    @Autowired
    private RentalOrderMapper orderMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Operation(summary = "获取首页统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(self.loadStats());
    }

    @Cacheable(value = "dashboardStats", key = "'public'")
    public Map<String, Object> loadStats() {
        Map<String, Object> stats = new HashMap<>();

        Long totalDrones = droneMapper.selectCount(new LambdaQueryWrapper<Drone>()
                .eq(Drone::getDeleted, 0));
        stats.put("totalDrones", totalDrones);

        Long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getDeleted, 0));
        stats.put("totalOrders", totalOrders);

        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 0)
                .eq(User::getDeleted, 0));
        stats.put("totalUsers", totalUsers);

        Long totalComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getDeleted, 0));
        stats.put("totalComments", totalComments);

        Long positiveComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getDeleted, 0)
                .ge(Comment::getRating, 4));
        if (totalComments > 0) {
            int rate = (int) (positiveComments * 100 / totalComments);
            stats.put("positiveRate", rate);
        } else {
            stats.put("positiveRate", 0);
        }

        return stats;
    }
}
