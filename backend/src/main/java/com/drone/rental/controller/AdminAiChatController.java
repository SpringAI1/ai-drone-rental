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
import java.util.stream.Collectors;

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
     * 获取会话列表（按会话聚合）
     */
    @Operation(summary = "获取会话列表（分页 + 聚合）")
    @GetMapping("/conversations")
    public Result<IPage<Map<String, Object>>> getConversations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键字（匹配消息内容）") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否只看有用户的（过滤匿名）") @RequestParam(required = false) Boolean withUserOnly) {

        List<AiChatMessage> all = aiChatMessageService.list();
        if (all == null) all = new ArrayList<>();

        // 按 conversationId 聚合
        Map<String, List<AiChatMessage>> byConv = all.stream()
                .collect(Collectors.groupingBy(AiChatMessage::getConversationId));

        List<Map<String, Object>> convs = new ArrayList<>();
        for (Map.Entry<String, List<AiChatMessage>> e : byConv.entrySet()) {
            List<AiChatMessage> msgs = e.getValue();
            if (msgs == null || msgs.isEmpty()) continue;

            LocalDateTime first = msgs.stream()
                    .map(AiChatMessage::getCreatedTime)
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime last = msgs.stream()
                    .map(AiChatMessage::getCreatedTime)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            Long userId = msgs.stream()
                    .map(AiChatMessage::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // 取第一条用户消息作为"问题"摘要
            String firstUserMsg = msgs.stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .sorted(Comparator.comparing(AiChatMessage::getCreatedTime))
                    .map(AiChatMessage::getContent)
                    .findFirst()
                    .orElse("");

            // 如果有关键字筛选：整条会话中是否有任何消息含关键字
            if (keyword != null && !keyword.trim().isEmpty()) {
                boolean hit = msgs.stream()
                        .anyMatch(m -> m.getContent() != null && m.getContent().contains(keyword));
                if (!hit) continue;
            }

            // 过滤：只看已登录用户
            if (Boolean.TRUE.equals(withUserOnly) && userId == null) continue;

            long userCount = msgs.stream().filter(m -> "user".equals(m.getRole())).count();
            long aiCount = msgs.stream().filter(m -> "ai".equals(m.getRole())).count();

            Map<String, Object> conv = new HashMap<>();
            conv.put("conversationId", e.getKey());
            conv.put("userId", userId);
            conv.put("firstMessage", truncate(firstUserMsg, 60));
            conv.put("messageCount", msgs.size());
            conv.put("userMessageCount", userCount);
            conv.put("aiMessageCount", aiCount);
            conv.put("firstTime", first);
            conv.put("lastTime", last);

            convs.add(conv);
        }

        // 按最后活跃时间倒序
        convs.sort((a, b) -> {
            LocalDateTime la = (LocalDateTime) a.get("lastTime");
            LocalDateTime lb = (LocalDateTime) b.get("lastTime");
            if (la == null && lb == null) return 0;
            if (la == null) return 1;
            if (lb == null) return -1;
            return lb.compareTo(la);
        });

        // 手动分页
        int total = convs.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pageRecords = (start < total) ? convs.subList(start, end) : new ArrayList<>();

        IPage<Map<String, Object>> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageRecords);

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
     * 获取整体统计（用于仪表盘/顶部汇总）
     */
    @Operation(summary = "获取对话统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        List<AiChatMessage> all = aiChatMessageService.list();
        if (all == null) all = new ArrayList<>();

        long totalMessages = all.size();
        long totalUserMessages = all.stream().filter(m -> "user".equals(m.getRole())).count();
        long totalSessions = all.stream()
                .map(AiChatMessage::getConversationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        long userCount = all.stream()
                .map(AiChatMessage::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", totalMessages);
        stats.put("totalUserMessages", totalUserMessages);
        stats.put("totalAiMessages", totalMessages - totalUserMessages);
        stats.put("totalSessions", totalSessions);
        stats.put("activeUserCount", userCount);
        stats.put("avgMessagesPerSession", totalSessions > 0 ? Math.round(100.0 * totalMessages / totalSessions) / 100.0 : 0);

        return Result.success(stats);
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }
}
