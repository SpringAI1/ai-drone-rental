package com.drone.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
@Schema(description = "AI聊天消息实体")
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "会话ID（UUID）")
    @TableField(value = "session_id")
    private String conversationId;

    @Schema(description = "用户ID（未登录时为空）")
    private Long userId;

    @Schema(description = "角色：user-用户，ai-AI助手")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "使用的模型")
    private String model;

    @Schema(description = "消耗的token数")
    @TableField(value = "token_count")
    private Integer tokens;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}