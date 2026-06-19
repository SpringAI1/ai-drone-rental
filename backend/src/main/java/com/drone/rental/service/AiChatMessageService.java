package com.drone.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.entity.AiChatMessage;

import java.util.List;

public interface AiChatMessageService extends IService<AiChatMessage> {

    void saveMessage(String conversationId, Long userId, String role, String content, String model);

    List<AiChatMessage> getRecentMessages(String conversationId, int limit);

    void clearConversation(String conversationId);
}