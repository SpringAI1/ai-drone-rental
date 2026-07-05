package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.mapper.AiChatMessageMapper;
import com.drone.rental.service.AiChatMessageService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage> implements AiChatMessageService {

    private static final int DEFAULT_MESSAGE_LIMIT = 20;
    private static final int MAX_MESSAGE_LIMIT = 200;

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
        if (limit > MAX_MESSAGE_LIMIT) {
            limit = MAX_MESSAGE_LIMIT;
        }
        // 使用 MyBatis Plus Page 替代 .last("LIMIT ...")，避免 SQL 注入
        Page<AiChatMessage> page = new Page<>(1, limit);
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getConversationId, conversationId)
                .orderByAsc(AiChatMessage::getCreatedTime);

        IPage<AiChatMessage> result = this.page(page, wrapper);
        return result.getRecords() != null ? result.getRecords() : Collections.emptyList();
    }

    @Override
    public void clearConversation(String conversationId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getConversationId, conversationId);
        this.remove(wrapper);
    }

    @Override
    public List<Map<String, Object>> getConversationSummaries(String keyword, Boolean withUserOnly,
                                                               int offset, int limit) {
        return this.baseMapper.findConversationSummaries(keyword, withUserOnly, offset, limit);
    }

    @Override
    public long countConversations(String keyword, Boolean withUserOnly) {
        return this.baseMapper.countConversations(keyword, withUserOnly);
    }

    @Override
    public Map<String, Object> getMessageStats() {
        Map<String, Object> stats = this.baseMapper.getMessageStats();
        if (stats == null) return Collections.emptyMap();
        return stats;
    }
}