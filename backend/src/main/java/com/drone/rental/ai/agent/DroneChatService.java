package com.drone.rental.ai.agent;

import com.drone.rental.ai.rag.RagService;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.AiChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 智能客服核心服务
 *
 * 完整能力链路：
 * 1) RAG：从向量知识库语义检索 top-3 相关文档
 * 2) Tool：注册 @Tool 注解的业务工具，LLM 按需调用
 * 3) Memory：InMemoryChatMemory 每会话独立保留上下文
 * 4) 智能下单：DomainOperationTools.smartCreateOrder
 * 5) 流式输出：Flux<String> 真正流式 + 打字机
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneChatService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final AiChatMessageService aiChatMessageService;

    private final java.util.Map<String, ChatMemory> memoryMap = new java.util.concurrent.ConcurrentHashMap<>();

    /** 最大会话数，超过后清理最早未使用的 */
    private static final int MAX_CONVERSATIONS = 1000;

    private static final String SYSTEM_PROMPT = """
            你是「翱翔无人机租赁平台」的 AI 智能客服，名字叫"小飞"。
            你专精于民用无人机租赁领域，熟悉中国民航局相关法规、各机型参数、空域申请、保险规则。
            回答时请遵循以下原则：
            1) 专业准确：优先使用工具查询真实业务数据，不确定时主动调用 queryAvailableDrones / queryOrderStatus / queryDroneDetail 等工具验证。
            2) 简洁清晰：回复控制在 200 字以内，使用 markdown 排版。
            3) 场景化推荐：根据用户预算/场景主动调用 recommendDroneByScenario 推荐。
            4) 业务边界：只能回答无人机租赁/法规/空域/订单/报修相关问题；其他话题礼貌引导回业务。
            5) 下单辅助：当用户表达"想租XX"时，可以主动调用 smartCreateOrder 引导下单（需先确认日期、地址、机型）。
            6) 引用出处：当 RAG 知识库返回了参考资料时，结尾标注"参考：《{来源}》"。
            7) 不可用兜底：如果工具/知识库都查不到，明确告知用户并建议联系人工客服 400-800-8888。
            """;

    private ChatMemory memoryFor(String conversationId) {
        // 防止内存泄漏：超过上限时清理一半旧会话
        if (memoryMap.size() >= MAX_CONVERSATIONS) {
            int toRemove = MAX_CONVERSATIONS / 2;
            java.util.Iterator<String> it = memoryMap.keySet().iterator();
            for (int i = 0; i < toRemove && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
            log.warn("[AI] ChatMemory 会话数达到上限，清理 {} 个旧会话", toRemove);
        }
        return memoryMap.computeIfAbsent(conversationId, k -> new InMemoryChatMemory());
    }

    private String resolveConversationId(String in) {
        return (in == null || in.isBlank()) ? UUID.randomUUID().toString().replace("-", "") : in;
    }

    private Long currentUserId() {
        try { return UserContext.getCurrentUserId(); } catch (Exception e) { return null; }
    }

    /** 给 LLM 工具调用透传 userId/username/role（解决异步线程 ThreadLocal 失效）
     *  永远至少包含一个 key（_init=true），否则 Spring AI MethodToolCallback 校验会抛
     *  "ToolContext is required by the method as an argument" 异常 */
    private Map<String, Object> buildToolContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_init", Boolean.TRUE);
        try {
            Long uid = UserContext.getCurrentUserId();
            if (uid != null) ctx.put("userId", uid);
            String un = UserContext.getCurrentUsername();
            if (un != null) ctx.put("username", un);
            Integer role = UserContext.getCurrentRole();
            if (role != null) ctx.put("role", role);
        } catch (Exception ignored) {}
        return ctx;
    }

    /**
     * 非流式对话 - 返回完整回答
     */
    public String chat(String userMessage, String conversationIdIn) {
        String conversationId = resolveConversationId(conversationIdIn);
        Long userId = currentUserId();
        ChatMemory mem = memoryFor(conversationId);
        ensureHistory(conversationId, mem);

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .toolContext(buildToolContext())
                .advisors(MessageChatMemoryAdvisor.builder(mem).build())
                .call()
                .content();
        if (answer == null) answer = "";
        persist(conversationId, userId, userMessage, answer);
        return answer;
    }

    /**
     * 流式对话 - SSE 打字机效果
     */
    public Flux<String> stream(String userMessage, String conversationIdIn) {
        String conversationId = resolveConversationId(conversationIdIn);
        Long userId = currentUserId();
        ChatMemory mem = memoryFor(conversationId);
        ensureHistory(conversationId, mem);

        // 预检 RAG（仅打日志 + 注入 system 提示，不阻塞流式）
        List<Document> ragDocs = ragService.search(userMessage, 3);
        log.info("[AI] stream 会话 {} 命中 RAG {} 块", conversationId, ragDocs.size());

        StringBuilder full = new StringBuilder();
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .toolContext(buildToolContext())
                .advisors(MessageChatMemoryAdvisor.builder(mem).build())
                .stream()
                .content()
                .doOnNext(full::append)
                .doOnComplete(() -> {
                    persist(conversationId, userId, userMessage, full.toString());
                    log.info("[AI] stream 会话 {} 完成，输出 {} 字", conversationId, full.length());
                })
                .doOnError(e -> log.error("[AI] stream 会话 {} 出错", conversationId, e));
    }

    /**
     * 把 DB 历史加载进 ChatMemory（保证重启后多轮上下文连续）
     */
    private void ensureHistory(String conversationId, ChatMemory mem) {
        try {
            List<AiChatMessage> history = aiChatMessageService.getRecentMessages(conversationId, 20);
            for (AiChatMessage msg : history) {
                String content = msg.getContent();
                if (content == null || content.isBlank()) continue;
                Message m = "user".equals(msg.getRole())
                        ? new UserMessage(content)
                        : new AssistantMessage(content);
                mem.add(conversationId, List.of(m));
            }
        } catch (Exception e) {
            log.warn("[AI] 加载历史消息失败: {}", e.getMessage());
        }
    }

    private void persist(String conversationId, Long userId, String userMessage, String aiMessage) {
        try {
            aiChatMessageService.saveMessage(conversationId, userId, "user", userMessage, "qwen-turbo");
            aiChatMessageService.saveMessage(conversationId, userId, "ai", aiMessage, "qwen-turbo");
        } catch (Exception e) {
            log.warn("[AI] 持久化消息失败: {}", e.getMessage());
        }
    }
}
