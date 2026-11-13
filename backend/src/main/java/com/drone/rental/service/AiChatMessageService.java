package com.drone.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.entity.AiChatMessage;

import java.util.List;
import java.util.Map;

public interface AiChatMessageService extends IService<AiChatMessage> {

    void saveMessage(String conversationId, Long userId, String role, String content, String model);

    List<AiChatMessage> getRecentMessages(String conversationId, int limit);

    void clearConversation(String conversationId);

    /** 按会话 + 用户隔离读取（未登录 userId=null 仅读匿名消息，防止越权读取他人对话） */
    List<AiChatMessage> getRecentMessages(String conversationId, Long userId, int limit);

    /** 按会话 + 用户隔离清空（仅删除当前用户自己的消息） */
    void clearConversation(String conversationId, Long userId);

    /** 分页查询会话摘要（按 conversation_id 聚合） */
    List<Map<String, Object>> getConversationSummaries(String keyword, Boolean withUserOnly, int offset, int limit);

    /** 统计会话总数 */
    long countConversations(String keyword, Boolean withUserOnly);

    /** 获取整体统计（不加载消息体） */
    Map<String, Object> getMessageStats();
}