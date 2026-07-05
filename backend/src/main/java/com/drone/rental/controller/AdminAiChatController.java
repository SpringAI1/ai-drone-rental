package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.rental.common.Result;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.service.AiChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话审计 — 管理端查看用户与 AI 的对话记录，用于反向优化本地知识库
 */
@Tag(name = "AI对话审计-管理端")
@RestController
@RequestMapping("/admin/ai-chat")
@RequiredArgsConstructor
public class AdminAiChatController {

    private final AiChatMessageService aiChatMessageService;

    /**
     * 获取会话列表（按会话聚合，数据库层分页 + 聚合）
     */
    @Operation(summary = "获取会话列表（分页 + 聚合）")
    @GetMapping("/conversations")
    public Result<IPage<Map<String, Object>>> getConversations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键字（匹配消息内容）") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否只看有用户的（过滤匿名）") @RequestParam(required = false) Boolean withUserOnly) {

        // 安全校验
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        int offset = (pageNum - 1) * pageSize;

        // 数据库层聚合 + 分页，不再全表加载到内存
        List<Map<String, Object>> summaries = aiChatMessageService.getConversationSummaries(
                keyword, withUserOnly, offset, pageSize);
        long total = aiChatMessageService.countConversations(keyword, withUserOnly);

        List<Map<String, Object>> convs = new ArrayList<>();
        for (Map<String, Object> row : summaries) {
            Map<String, Object> conv = new HashMap<>();
            conv.put("conversationId", row.get("conversation_id"));
            conv.put("userId", row.get("user_id"));
            conv.put("messageCount", row.get("message_count"));
            conv.put("userMessageCount", row.get("user_message_count"));
            conv.put("aiMessageCount", row.get("ai_message_count"));
            conv.put("firstTime", row.get("first_time"));
            conv.put("lastTime", row.get("last_time"));
            conv.put("firstMessage", ""); // 详情页再加载首条消息
            convs.add(conv);
        }

        IPage<Map<String, Object>> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(convs);

        return Result.success(page);
    }

    /**
     * 获取单会话详细对话（按时间正序）
     */
    @Operation(summary = "获取单会话对话详情")
    @GetMapping("/conversation/{conversationId}")
    public Result<Map<String, Object>> getConversationDetail(
            @Parameter(description = "会话ID") @PathVariable String conversationId) {

        List<AiChatMessage> list = aiChatMessageService.getRecentMessages(conversationId, 200);
        if (list == null) list = new ArrayList<>();

        list.sort(Comparator.comparing(m -> m.getCreatedTime() == null ? LocalDateTime.MIN : m.getCreatedTime()));

        List<Map<String, Object>> messages = new ArrayList<>();
        String firstUserMsg = "";
        Long userId = null;
        LocalDateTime firstTime = null;
        LocalDateTime lastTime = null;

        for (AiChatMessage m : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("role", m.getRole());
            row.put("content", m.getContent());
            row.put("model", m.getModel());
            row.put("time", m.getCreatedTime());
            messages.add(row);

            if (userId == null && m.getUserId() != null) userId = m.getUserId();
            if ("user".equals(m.getRole()) && firstUserMsg.isEmpty()) {
                firstUserMsg = m.getContent();
            }
            if (firstTime == null && m.getCreatedTime() != null) firstTime = m.getCreatedTime();
            if (m.getCreatedTime() != null) lastTime = m.getCreatedTime();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("userId", userId);
        result.put("firstMessage", truncate(firstUserMsg, 60));
        result.put("messageCount", messages.size());
        result.put("firstTime", firstTime);
        result.put("lastTime", lastTime);
        result.put("messages", messages);

        return Result.success(result);
    }

    /**
     * 删除整个会话
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable String conversationId) {
        aiChatMessageService.clearConversation(conversationId);
        return Result.success();
    }

    /**
     * 获取整体统计（用于仪表盘/顶部汇总），数据库层聚合不加载消息体
     */
    @Operation(summary = "获取对话统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = aiChatMessageService.getMessageStats();
        if (stats == null) stats = new HashMap<>();

        // 补充计算字段
        long totalMessages = stats.get("total_messages") instanceof Number
                ? ((Number) stats.get("total_messages")).longValue() : 0;
        long totalUserMessages = stats.get("total_user_messages") instanceof Number
                ? ((Number) stats.get("total_user_messages")).longValue() : 0;
        long totalSessions = stats.get("total_sessions") instanceof Number
                ? ((Number) stats.get("total_sessions")).longValue() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalMessages", totalMessages);
        result.put("totalUserMessages", totalUserMessages);
        result.put("totalAiMessages", totalMessages - totalUserMessages);
        result.put("totalSessions", totalSessions);
        result.put("activeUserCount", stats.getOrDefault("active_user_count", 0));
        result.put("avgMessagesPerSession", totalSessions > 0
                ? Math.round(100.0 * totalMessages / totalSessions) / 100.0 : 0);

        return Result.success(result);
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }
}
