package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.Constants;
import com.drone.rental.common.ResultCode;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dto.OrderCreateDTO;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.Payment;
import com.drone.rental.entity.RentalOrder;
import com.drone.rental.entity.User;
import com.drone.rental.mapper.RentalOrderMapper;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.*;
import com.drone.rental.service.support.OrderConverter;
import com.drone.rental.service.support.OrderNotifier;
import com.drone.rental.service.support.OrderNumberGenerator;
import com.drone.rental.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl extends ServiceImpl<RentalOrderMapper, RentalOrder> implements OrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private DroneService droneService;

    @Autowired
    private AirspaceRecordService airspaceRecordService;

    @Autowired
    @Lazy
    private PaymentService paymentService;

    @Autowired
    private UserQualificationService qualificationService;

    @Autowired
    private OrderConverter orderConverter;

    @Autowired
    private OrderNumberGenerator orderNumberGenerator;

    @Autowired
    private OrderNotifier orderNotifier;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        Long userId = UserContext.getCurrentUserId();

        // 1. 校验用户状态 / 资质 / 机型可租 / 空域备案
        userService.checkUserOperable(userId);
        qualificationService.checkQualificationValid(userId);
        droneService.checkDroneRentable(dto.getDroneId());
        if (dto.getAirspaceRecordId() != null) {
            airspaceRecordService.checkAirspaceRecordValid(dto.getAirspaceRecordId(), userId);
        }

        // 2. 计算租期 / 金额
        Drone drone = droneService.getById(dto.getDroneId());
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (days <= 0) {
            throw new BusinessException("租赁结束日期必须大于等于开始日期");
        }
        BigDecimal totalAmount = drone.getPricePerDay().multiply(BigDecimal.valueOf(days));

        // 3. 落库
        RentalOrder order = new RentalOrder();
        order.setOrderNo(orderNumberGenerator.generate());
        order.setUserId(userId);
        order.setDroneId(dto.getDroneId());
        order.setAirspaceRecordId(dto.getAirspaceRecordId());
        order.setRentalStartTime(dto.getStartDate().atStartOfDay());
        order.setRentalEndTime(dto.getEndDate().atTime(23, 59, 59));
        order.setRentalDays((int) days);
        order.setUnitPrice(drone.getPricePerDay());
        order.setTotalAmount(totalAmount);
        order.setDepositAmount(BigDecimal.ZERO);
        order.setOrderStatus(Constants.ORDER_STATUS_UNPAID);
        if (dto.getDeliveryAddress() != null && !dto.getDeliveryAddress().trim().isEmpty()) {
            order.setDeliveryAddress(dto.getDeliveryAddress().trim());
        }
        order.setRemark(dto.getRemark());
        this.save(order);

        // 4. 通知（失败不影响主流程）
        orderNotifier.sendCreateNotifications(order, drone, totalAmount);

        // 5. 扣库存 + 创建支付记录
        droneService.decreaseStock(dto.getDroneId(), 1, order.getId());
        paymentService.createPayment(order);

        return orderConverter.convertToVO(order);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        // 普通用户只能查看自己的订单
        if (!UserContext.isAdmin() && !order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return orderConverter.convertToVO(order);
    }

    @Override
    public IPage<OrderVO> getCurrentUserOrders(Integer pageNum, Integer pageSize, Integer orderStatus) {
        Long userId = UserContext.getCurrentUserId();
        Page<RentalOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getUserId, userId);
        if (orderStatus != null) {
            wrapper.eq(RentalOrder::getOrderStatus, orderStatus);
        }
        wrapper.orderByDesc(RentalOrder::getCreatedTime);
        IPage<RentalOrder> orderPage = this.page(page, wrapper);
        // 批量加载关联数据，避免 N+1
        List<OrderVO> voList = orderConverter.convertBatch(orderPage.getRecords());
        IPage<OrderVO> voPage = new Page<>(pageNum, pageSize, orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<OrderVO> pageOrders(Integer pageNum, Integer pageSize, String orderNo,
                                     String userPhone, Integer orderStatus, String startDate, String endDate) {
        Page<RentalOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(RentalOrder::getOrderNo, orderNo);
        }
        if (orderStatus != null) {
            wrapper.eq(RentalOrder::getOrderStatus, orderStatus);
        }
        if (StringUtils.hasText(userPhone)) {
            List<Long> userIds = userService.list(
                    new LambdaQueryWrapper<User>().like(User::getPhone, userPhone)
            ).stream().map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(RentalOrder::getUserId, userIds);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(RentalOrder::getCreatedTime, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(RentalOrder::getCreatedTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(RentalOrder::getCreatedTime);

        IPage<RentalOrder> orderPage = this.page(page, wrapper);
        List<OrderVO> voList = orderConverter.convertBatch(orderPage.getRecords());
        IPage<OrderVO> voPage = new Page<>(pageNum, pageSize, orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void simulatePay(Long orderId, String deliveryAddress, Integer paymentMethod) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_PAID);
        }
        if (deliveryAddress != null && !deliveryAddress.trim().isEmpty()) {
            order.setDeliveryAddress(deliveryAddress.trim());
        }
        if (paymentMethod != null) {
            order.setPaymentMethod(paymentMethod);
        }
        order.setOrderStatus(Constants.ORDER_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);

        // 支付记录幂等创建
        Payment existingPayment = paymentService.getByOrderId(orderId);
        if (existingPayment == null) {
            paymentService.createPayment(order);
        }
        paymentService.updatePaymentStatus(orderId, Constants.PAYMENT_STATUS_PAID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!UserContext.isAdmin() && !order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Integer status = order.getOrderStatus();
        boolean canCancel = Objects.equals(status, Constants.ORDER_STATUS_UNPAID)
                || Objects.equals(status, Constants.ORDER_STATUS_PAID)
                || Objects.equals(status, Constants.ORDER_STATUS_SHIPPED);
        if (!canCancel) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_CANCEL);
        }
        // 已支付/已发货的订单取消时退还余额 + 更新支付记录
        if (Objects.equals(status, Constants.ORDER_STATUS_PAID)
                || Objects.equals(status, Constants.ORDER_STATUS_SHIPPED)) {
            if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                userService.increaseBalance(order.getUserId(), order.getTotalAmount());
            }
            paymentService.refundPayment(orderId, reason != null ? reason : "取消订单自动退款");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_CANCELLED);
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        this.updateById(order);

        droneService.increaseStock(order.getDroneId(), 1, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(Long orderId) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_RENTING) {
            throw new BusinessException("订单状态不允许确认归还，只有租赁中的订单才能确认归还");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_RETURNED);
        order.setReturnTime(LocalDateTime.now());
        this.updateById(order);
        droneService.increaseStock(order.getDroneId(), 1, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, String reason) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_PAID) {
            throw new BusinessException("订单状态不允许退款");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_REFUNDED);
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        this.updateById(order);
        // 退还余额给用户
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            userService.increaseBalance(order.getUserId(), order.getTotalAmount());
        }
        paymentService.refundPayment(orderId, reason);
        droneService.increaseStock(order.getDroneId(), 1, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String expressCompany, String expressNo) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_PAID) {
            throw new BusinessException("订单状态不允许发货，只有已支付的订单才能发货");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_SHIPPED);
        order.setRemark("快递公司: " + expressCompany + ", 快递单号: " + expressNo);
        order.setShipTime(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long orderId) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_SHIPPED) {
            throw new BusinessException("订单状态不允许确认收货");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_RENTING);
        order.setReceiveTime(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReturn(Long orderId) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_RENTING) {
            throw new BusinessException("订单状态不允许申请退租");
        }
        // 用户自助退租：标记为已归还 + 记录归还时间 + 归还库存
        order.setOrderStatus(Constants.ORDER_STATUS_RETURNED);
        order.setReturnTime(LocalDateTime.now());
        this.updateById(order);
        droneService.increaseStock(order.getDroneId(), 1, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long orderId, Integer status) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        Integer currentStatus = order.getOrderStatus();
        if (status.equals(currentStatus)) {
            return;
        }
        switch (status) {
            case Constants.ORDER_STATUS_PAID:
                if (currentStatus != Constants.ORDER_STATUS_UNPAID) {
                    throw new BusinessException("只有待支付的订单可以更新为已支付");
                }
                break;
            case Constants.ORDER_STATUS_SHIPPED:
                if (currentStatus != Constants.ORDER_STATUS_PAID) {
                    throw new BusinessException("只有已支付的订单可以更新为已发货");
                }
                break;
            case Constants.ORDER_STATUS_RENTING:
                if (currentStatus != Constants.ORDER_STATUS_SHIPPED) {
                    throw new BusinessException("只有已发货的订单可以更新为租赁中");
                }
                break;
            case Constants.ORDER_STATUS_RETURNED:
                if (currentStatus != Constants.ORDER_STATUS_RENTING) {
                    throw new BusinessException("只有租赁中的订单可以更新为已归还");
                }
                droneService.increaseStock(order.getDroneId(), 1, orderId);
                break;
            case Constants.ORDER_STATUS_CANCELLED:
                if (currentStatus != Constants.ORDER_STATUS_UNPAID) {
                    throw new BusinessException("只有待支付的订单可以更新为已取消");
                }
                droneService.increaseStock(order.getDroneId(), 1, orderId);
                break;
            case Constants.ORDER_STATUS_REFUNDED:
                if (currentStatus != Constants.ORDER_STATUS_PAID) {
                    throw new BusinessException("只有已支付的订单可以更新为已退款");
                }
                if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    userService.increaseBalance(order.getUserId(), order.getTotalAmount());
                }
                paymentService.refundPayment(orderId, "管理员手动退款");
                droneService.increaseStock(order.getDroneId(), 1, orderId);
                break;
            default:
                throw new BusinessException("无效的订单状态值");
        }
        order.setOrderStatus(status);
        this.updateById(order);
    }

    @Override
    public Map<String, Object> getCurrentUserOrderStats() {
        Long userId = UserContext.getCurrentUserId();
        Map<String, Object> stats = new HashMap<>();
        LambdaQueryWrapper<RentalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalOrder::getUserId, userId);

        stats.put("pendingPay", this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_UNPAID)));
        stats.put("pendingShip", this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_PAID)));
        stats.put("pendingReceive", this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_SHIPPED)));
        stats.put("renting", this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_RENTING)));
        stats.put("returned", this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_RETURNED)));
        stats.put("canceled",
                this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_CANCELLED))
                        + this.count(wrapper.clone().eq(RentalOrder::getOrderStatus, Constants.ORDER_STATUS_REFUNDED)));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long orderId, String reason) {
        RentalOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_PAID
                && order.getOrderStatus() != Constants.ORDER_STATUS_SHIPPED) {
            throw new BusinessException("只有已支付或已发货的订单可以申请退款");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_REFUNDED);
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        this.updateById(order);
        // 退还余额给用户
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            userService.increaseBalance(order.getUserId(), order.getTotalAmount());
        }
        paymentService.refundPayment(orderId, reason);
        droneService.increaseStock(order.getDroneId(), 1, orderId);
    }
}
