package com.drone.rental.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 评论DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "评论参数")
public class CommentDTO {

    @NotNull(message = "无人机ID不能为空")
    @Schema(description = "无人机ID")
    private Long droneId;

    @Schema(description = "订单ID")
    private Long orderId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;

    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    @Schema(description = "评分: 1-5星")
    private Integer rating;

    @Schema(description = "评论图片(JSON数组)")
    private List<String> images;

    @Schema(description = "父评论ID，用于二级评论")
    private Long parentId;
}
