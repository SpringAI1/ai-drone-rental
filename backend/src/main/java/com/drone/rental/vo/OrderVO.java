package com.drone.rental.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息VO
 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "无人机ID")
    private Long droneId;

    @Schema(description = "无人机型号")
    private String droneModel;

    @Schema(description = "无人机图片")
    private String droneImage;

    @Schema(description = "空域备案ID")
    private Long airspaceRecordId;

    @Schema(description = "飞行区域名称")
    private String regionName;

    @Schema(description = "租赁开始时间")
    private LocalDateTime rentalStartTime;

    @Schema(description = "租赁结束时间")
    private LocalDateTime rentalEndTime;

    @Schema(description = "租赁天数")
    private Integer rentalDays;

    @Schema(description = "单价(元/天)")
    private BigDecimal unitPrice;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "押金金额")
    private BigDecimal depositAmount;

    @Schema(description = "订单状态: 0-待支付, 1-已支付, 2-租赁中, 3-已归还, 4-已取消, 5-已退款")
    private Integer orderStatus;

    @Schema(description = "订单状态描述")
    private String orderStatusDesc;

    @Schema(description = "收货地址")
    private String deliveryAddress;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "是否已评论")
    private Boolean hasComment;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /**
     * 获取订单状态描述
     */
    public String getOrderStatusDesc() {
        if (orderStatus == null) return "";
        switch (orderStatus) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "租赁中";
            case 3: return "已归还";
            case 4: return "已取消";
            case 5: return "已退款";
            default: return "未知";
        }
    }
}
