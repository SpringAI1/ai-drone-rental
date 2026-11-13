package com.drone.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 订单创建DTO
 */
@Data
@Schema(description = "订单创建参数")
public class OrderCreateDTO {

    @NotNull(message = "无人机ID不能为空")
    @Schema(description = "无人机ID")
    private Long droneId;

    @Schema(description = "空域备案ID")
    private Long airspaceRecordId;

    @NotNull(message = "租赁开始日期不能为空")
    @Schema(description = "租赁开始日期")
    private LocalDate startDate;

    @NotNull(message = "租赁结束日期不能为空")
    @Schema(description = "租赁结束日期")
    private LocalDate endDate;

    @Schema(description = "收货地址")
    private String deliveryAddress;

    @Schema(description = "订单备注")
    private String remark;

    /**
     * 下单用户ID：AI 工具通过 ToolContext 解析后透传到这里，
     * 用于解决工具调用在异步线程执行时 ThreadLocal (UserContext) 失效的问题。
     * 标记 READ_ONLY：仅允许 Java 代码 setter 赋值，忽略客户端 JSON 传入的 userId，防止越权下单。
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "下单用户ID（AI 工具内部透传，不对外暴露）", hidden = true)
    private Long userId;
}
