package com.drone.rental.controller;

import com.drone.rental.ai.agent.DroneChatService;
import com.drone.rental.ai.rag.RagService;
import com.drone.rental.ai.tools.DomainOperationTools;
import com.drone.rental.ai.tools.DroneQueryTools;
import com.drone.rental.common.Result;
import com.drone.rental.dto.AiStatusDTO;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.service.AiChatMessageService;
import com.drone.rental.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 翱翔无人机 AI 智能客服 v3 控制器
 *
 * 真正实现：
 * 1) RAG 语义检索（基于 Redis 向量库）
 * 2) @Tool 注解业务工具（LLM Function Calling）
 * 3) ChatMemory 多轮记忆
 * 4) SSE 流式响应（打字机）
 * 5) MCP Server 协议（业务工具通过 /api/mcp/sse 暴露给 Claude Desktop/Cursor）
 *
 * 端点：
 * - POST /ai/v3/chat          普通对话
 * - POST /ai/v3/chat/stream   流式对话（SSE）
 * - GET  /ai/v3/history/{id}  历史
 * - DELETE /ai/v3/conversation/{id}  清空
 * - GET  /ai/v3/status        服务状态
 * - PUT  /ai/v3/admin/status  维护开关
 * - GET  /ai/v3/rag/search    知识库语义检索
 * - GET  /ai/v3/tools         列出可用工具
 */
@Slf4j
@RestController
@RequestMapping("/ai/v3")
@RequiredArgsConstructor
public class AiChatController {

    private final DroneChatService droneChatService;
    private final RagService ragService;
    private final DroneQueryTools droneQueryTools;
    private final DomainOperationTools domainOperationTools;
    private final AiChatMessageService aiChatMessageService;
    private final AiConfigService aiConfigService;

    @PostMapping("/chat")
    @Operation(summary = "AI 智能对话 - RAG + Tool + Memory 完整链路")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> req) {
        if (!aiConfigService.getAiStatus()) {
            return Result.error(aiConfigService.getAiMaintenanceMessage());
        }
        String message = req.get("message");
        if (message == null || message.isBlank()) return Result.error("消息内容不能为空");

        String conversationId = req.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        long start = System.currentTimeMillis();
        String reply = droneChatService.chat(message, conversationId);
        long cost = System.currentTimeMillis() - start;
        log.info("[AI] 非流式会话 {} 耗时 {}ms, 输入 {} 字, 输出 {} 字",
                conversationId, cost, message.length(), reply.length());

        Map<String, String> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("reply", reply);
        data.put("elapsedMs", String.valueOf(cost));
        return Result.success(data);
    }

    /**
     * SSE 流式对话：MIME 是 text/event-stream
     * 客户端 EventSource('http://localhost:8080/api/ai/v3/chat/stream', { withCredentials: true })
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 流式对话 - SSE 打字机效果")
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestBody Map<String, String> req,
            HttpServletResponse response) {
        if (!aiConfigService.getAiStatus()) {
            return Flux.error(new RuntimeException(aiConfigService.getAiMaintenanceMessage()));
        }
        String message = req.get("message");
        if (message == null || message.isBlank()) {
            return Flux.error(new RuntimeException("消息内容不能为空"));
        }
        String conversationId = req.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        // 防止代理缓冲
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        String cid = conversationId;
        return droneChatService.stream(message, conversationId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .id(cid)
                        .event("message")
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "获取对话历史")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable String conversationId) {
        List<AiChatMessage> messages = aiChatMessageService.getRecentMessages(conversationId, 50);
        List<Map<String, Object>> result = messages.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("createdTime", m.getCreatedTime());
                    return map;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "清空指定对话")
    public Result<Void> clearConversation(@PathVariable String conversationId) {
        aiChatMessageService.clearConversation(conversationId);
        return Result.success();
    }

    @GetMapping("/status")
    @Operation(summary = "AI 服务状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", aiConfigService.getAiStatus());
        data.put("maintenanceMessage", aiConfigService.getAiMaintenanceMessage());
        data.put("model", "qwen-turbo");
        data.put("rag", Map.of("enabled", true, "store", "SimpleVectorStore(in-memory)", "embedding", "text-embedding-v3"));
        data.put("tools", List.of(
                "queryAvailableDrones", "queryDroneDetail", "recommendDroneByScenario",
                "queryUserOrders", "queryOrderStatus", "queryMaintenanceRecords",
                "smartCreateOrder", "simulatePayOrder", "queryMyQualification"
        ));
        data.put("mcp", Map.of("enabled", true, "endpoint", "/api/mcp/sse"));
        return Result.success(data);
    }

    @PutMapping("/admin/status")
    @Operation(summary = "管理员：更新 AI 状态")
    public Result<Void> updateStatus(@Valid @RequestBody AiStatusDTO dto) {
        aiConfigService.setAiStatus(dto.getEnabled());
        if (dto.getMaintenanceMessage() != null && !dto.getMaintenanceMessage().isEmpty()) {
            aiConfigService.setAiMaintenanceMessage(dto.getMaintenanceMessage());
        }
        return Result.success();
    }

    @GetMapping("/rag/search")
    @Operation(summary = "知识库语义检索（调试用）")
    public Result<List<Map<String, Object>>> searchRag(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        List<Map<String, Object>> result = ragService.search(query, topK).stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("text", d.getText());
                    m.put("metadata", d.getMetadata());
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/tools")
    @Operation(summary = "列出可用工具（LLM Function Calling 列表）")
    public Result<List<Map<String, Object>>> getTools() {
        return Result.success(List.of(
                Map.of("name", "queryAvailableDrones", "description", "查询可租赁无人机库存（按品牌/类型/价格/库存）"),
                Map.of("name", "queryDroneDetail", "description", "查询无人机详细信息"),
                Map.of("name", "recommendDroneByScenario", "description", "按场景推荐无人机"),
                Map.of("name", "queryUserOrders", "description", "查询用户订单"),
                Map.of("name", "queryOrderStatus", "description", "查询订单详细状态"),
                Map.of("name", "queryMaintenanceRecords", "description", "查询维修工单记录"),
                Map.of("name", "smartCreateOrder", "description", "AI 智能下单（含资质/库存/日期校验）"),
                Map.of("name", "simulatePayOrder", "description", "模拟支付订单"),
                Map.of("name", "queryMyQualification", "description", "查询当前用户飞行资质")
        ));
    }
}
