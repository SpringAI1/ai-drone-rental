package com.drone.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.entity.AiChatMessage;

import java.util.List;
import java.util.Map;

public interface AiChatMessageService extends IService<AiChatMessage> {

    void saveMessage(String conversationId, Long userId, String role, String content, String model);

    List<AiChatMessage> getRecentMessages(String conversationId, int limit);

    void clearConversation(String conversationId);

    /** 分页查询会话摘要（按 conversation_id 聚合） */
    List<Map<String, Object>> getConversationSummaries(String keyword, Boolean withUserOnly, int offset, int limit);

    /** 统计会话总数 */
    long countConversations(String keyword, Boolean withUserOnly);

    /** 获取整体统计（不加载消息体） */
    Map<String, Object> getMessageStats();
}