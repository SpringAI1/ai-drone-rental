package com.drone.rental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "AI状态更新参数")
public class AiStatusDTO {

    @NotNull(message = "状态不能为空")
    @Schema(description = "AI状态: true-启用, false-维护中")
    private Boolean enabled;

    @Schema(description = "维护提示消息")
    private String maintenanceMessage;
}
