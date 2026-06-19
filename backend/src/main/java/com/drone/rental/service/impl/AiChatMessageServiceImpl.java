package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.mapper.AiChatMessageMapper;
import com.drone.rental.service.AiChatMessageService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage> implements AiChatMessageService {

    private static final int DEFAULT_MESSAGE_LIMIT = 20;

    @Override
    public void saveMessage(String conversationId, Long userId, String role, String content, String model) {
        AiChatMessage message = new AiChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setModel(model);
        this.save(message);
    }

    @Override
    public List<AiChatMessage> getRecentMessages(String conversationId, int limit) {
        if (limit <= 0) {
            limit = DEFAULT_MESSAGE_LIMIT;
        }
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getConversationId, conversationId)
                .orderByAsc(AiChatMessage::getCreatedTime)
                .last("LIMIT " + limit);

        List<AiChatMessage> messages = this.list(wrapper);
        return messages != null ? messages : Collections.emptyList();
    }

    @Override
    public void clearConversation(String conversationId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getConversationId, conversationId);
        this.remove(wrapper);
    }
}