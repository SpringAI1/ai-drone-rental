package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.rental.common.Result;
import com.drone.rental.entity.*;
import com.drone.rental.mapper.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Tag(name = "管理后台-控制台")
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    @Lazy
    private AdminDashboardController self;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DroneMapper droneMapper;

    @Autowired
    private RentalOrderMapper orderMapper;

    @Autowired
    private UserQualificationMapper qualificationMapper;

    @Autowired
    private FaultReportMapper faultReportMapper;

    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(self.loadStats());
    }

    @Cacheable(value = "dashboardStats", key = "'admin'")
    public Map<String, Object> loadStats() {
        Map<String, Object> stats = new HashMap<>();

        // 今日开始时间
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);

        // 昨日时间范围
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayStart = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime yesterdayEnd = LocalDateTime.of(yesterday, LocalTime.MAX);

        // 今日收入（已支付订单）
        LambdaQueryWrapper<RentalOrder> todayRevenueQuery = new LambdaQueryWrapper<>();
        todayRevenueQuery.between(RentalOrder::getCreatedTime, todayStart, todayEnd)
                   .in(RentalOrder::getOrderStatus, 1, 2, 3, 5);
        List<RentalOrder> todayOrderList = orderMapper.selectList(todayRevenueQuery);
        BigDecimal todayRevenue = todayOrderList.stream()
                .map(RentalOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayRevenue", todayRevenue);
        stats.put("todayOrders", todayOrderList.size());

        // 昨日收入和订单数（用于计算环比）
        LambdaQueryWrapper<RentalOrder> yesterdayRevenueQuery = new LambdaQueryWrapper<>();
        yesterdayRevenueQuery.between(RentalOrder::getCreatedTime, yesterdayStart, yesterdayEnd)
                   .in(RentalOrder::getOrderStatus, 1, 2, 3, 5);
        List<RentalOrder> yesterdayOrderList = orderMapper.selectList(yesterdayRevenueQuery);
        BigDecimal yesterdayRevenue = yesterdayOrderList.stream()
                .map(RentalOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算收入环比增长率
        if (yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal revenueTrend = todayRevenue.subtract(yesterdayRevenue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(yesterdayRevenue, 0, java.math.RoundingMode.HALF_UP);
            stats.put("revenueTrend", revenueTrend.intValue());
        } else {
            stats.put("revenueTrend", todayRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0);
        }

        // 计算订单环比增长率
        int yesterdayOrderCount = yesterdayOrderList.size();
        if (yesterdayOrderCount > 0) {
            int orderTrend = (todayOrderList.size() - yesterdayOrderCount) * 100 / yesterdayOrderCount;
            stats.put("orderTrend", orderTrend);
        } else {
            stats.put("orderTrend", todayOrderList.size() > 0 ? 100 : 0);
        }

        // 总用户数
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 0)
                .eq(User::getDeleted, 0));
        stats.put("totalUsers", totalUsers);

        // 设备总数
        Long totalDrones = droneMapper.selectCount(new LambdaQueryWrapper<Drone>()
                .eq(Drone::getDeleted, 0));
        stats.put("totalDrones", totalDrones);

        return stats;
    }

    @Operation(summary = "获取收入趋势")
    @GetMapping("/revenue-trend")
    public Result<Map<String, Object>> getRevenueTrend(@RequestParam(defaultValue = "week") String period) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        LocalDate today = LocalDate.now();

        if ("year".equals(period)) {
            int currentYear = today.getYear();
            for (int month = 1; month <= 12; month++) {
                dates.add(month + "月");
                LocalDate monthStart = LocalDate.of(currentYear, month, 1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                LocalDateTime startTime = LocalDateTime.of(monthStart, LocalTime.MIN);
                LocalDateTime endTime = LocalDateTime.of(monthEnd, LocalTime.MAX);

                LambdaQueryWrapper<RentalOrder> query = new LambdaQueryWrapper<>();
                query.between(RentalOrder::getCreatedTime, startTime, endTime)
                     .in(RentalOrder::getOrderStatus, 1, 2, 3, 5);
                List<RentalOrder> orders = orderMapper.selectList(query);
                BigDecimal monthRevenue = orders.stream()
                        .map(RentalOrder::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                values.add(monthRevenue);
            }
        } else {
            int days = "week".equals(period) ? 7 : 30;
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.toString().substring(5));
                LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

                LambdaQueryWrapper<RentalOrder> query = new LambdaQueryWrapper<>();
                query.between(RentalOrder::getCreatedTime, dayStart, dayEnd)
                     .in(RentalOrder::getOrderStatus, 1, 2, 3, 5);
                List<RentalOrder> orders = orderMapper.selectList(query);
                BigDecimal dayRevenue = orders.stream()
                        .map(RentalOrder::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                values.add(dayRevenue);
            }
        }

        result.put("dates", dates);
        result.put("values", values);
        return Result.success(result);
    }

    @Operation(summary = "获取订单趋势")
    @GetMapping("/order-trend")
    public Result<Map<String, Object>> getOrderTrend(@RequestParam(defaultValue = "week") String period) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        int days = 7;
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.toString().substring(5));
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = orderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                    .between(RentalOrder::getCreatedTime, dayStart, dayEnd));
            values.add(count);
        }

        result.put("dates", dates);
        result.put("values", values);
        return Result.success(result);
    }

    @Operation(summary = "获取热门设备")
    @GetMapping("/popular-drones")
    public Result<List<Map<String, Object>>> getPopularDrones(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(self.loadPopularDrones(limit));
    }

    @Cacheable(value = "popularDrones", key = "#limit")
    public List<Map<String, Object>> loadPopularDrones(Integer limit) {
        List<Map<String, Object>> list = new ArrayList<>();

        List<Drone> drones = droneMapper.selectList(new LambdaQueryWrapper<Drone>()
                .eq(Drone::getDeleted, 0)
                .eq(Drone::getOnShelf, 1)
                .last("LIMIT " + limit));

        if (drones.isEmpty()) {
            return list;
        }

        List<Long> droneIds = drones.stream().map(Drone::getId).collect(java.util.stream.Collectors.toList());
        List<RentalOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .in(RentalOrder::getDroneId, droneIds));

        Map<Long, Long> rentCountMap = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        RentalOrder::getDroneId,
                        java.util.stream.Collectors.counting()));

        for (Drone drone : drones) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", drone.getId());
            item.put("model", drone.getModel());
            item.put("brand", drone.getBrand());
            item.put("rentCount", rentCountMap.getOrDefault(drone.getId(), 0L));
            list.add(item);
        }

        list.sort((a, b) -> ((Long)b.get("rentCount")).compareTo((Long)a.get("rentCount")));
        return list;
    }

    @Operation(summary = "获取最新订单")
    @GetMapping("/recent-orders")
    public Result<List<Map<String, Object>>> getRecentOrders(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(self.loadRecentOrders(limit));
    }

    @Cacheable(value = "recentOrders", key = "#limit")
    public List<Map<String, Object>> loadRecentOrders(Integer limit) {
        List<Map<String, Object>> list = new ArrayList<>();

        List<RentalOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<RentalOrder>()
                .orderByDesc(RentalOrder::getCreatedTime)
                .last("LIMIT " + limit));

        if (orders.isEmpty()) {
            return list;
        }

        List<Long> userIds = orders.stream().map(RentalOrder::getUserId).collect(java.util.stream.Collectors.toList());
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds));

        Map<Long, String> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(
                        User::getId,
                        User::getNickname));

        for (RentalOrder order : orders) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("status", order.getOrderStatus());
            item.put("totalAmount", order.getTotalAmount());
            item.put("userName", userMap.getOrDefault(order.getUserId(), "未知"));
            list.add(item);
        }

        return list;
    }

    @Operation(summary = "获取待办事项统计")
    @GetMapping("/todos")
    public Result<Map<String, Object>> getTodos() {
        Map<String, Object> todos = new HashMap<>();

        Long pendingAudit = qualificationMapper.selectCount(new LambdaQueryWrapper<UserQualification>()
                .eq(UserQualification::getAuditStatus, 0)
                .eq(UserQualification::getDeleted, 0));
        todos.put("audit", pendingAudit);

        Long pendingShip = orderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderStatus, 1));
        todos.put("ship", pendingShip);

        Long pendingReturn = orderMapper.selectCount(new LambdaQueryWrapper<RentalOrder>()
                .eq(RentalOrder::getOrderStatus, 4));
        todos.put("return", pendingReturn);

        Long pendingMaintenance = faultReportMapper.selectCount(new LambdaQueryWrapper<FaultReport>()
                .eq(FaultReport::getAuditStatus, 0)
                .eq(FaultReport::getDeleted, 0));
        todos.put("maintenance", pendingMaintenance);

        return Result.success(todos);
    }
}
