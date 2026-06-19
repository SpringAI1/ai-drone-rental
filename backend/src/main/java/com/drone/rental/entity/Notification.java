package com.drone.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("notification")
@Schema(description = "通知实体")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "通知类型: 1-订单通知, 2-审核通知, 3-系统通知")
    private Integer type;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "关联业务ID")
    private Long businessId;

    @Schema(description = "是否已读: 0-未读, 1-已读")
    private Integer readStatus;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "逻辑删除标志")
    @TableLogic
    private Integer deleted;
}