-- AI聊天消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID（UUID）',
    `user_id` BIGINT NULL COMMENT '用户ID（未登录时为空）',
    `role` VARCHAR(16) NOT NULL COMMENT '角色：user-用户，ai-AI助手',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `model` VARCHAR(64) NULL COMMENT '使用的模型',
    `tokens` INT NULL COMMENT '消耗的token数',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';