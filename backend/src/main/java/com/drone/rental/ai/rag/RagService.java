package com.drone.rental.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;

    @Value("classpath:knowledge-base/drone_rental_rules.txt")
    private Resource rentalRulesResource;

    @Value("classpath:knowledge-base/civil_aviation_regulations.txt")
    private Resource aviationRegulationsResource;

    @Value("classpath:knowledge-base/drone_insurance_regulations.txt")
    private Resource insuranceRegulationsResource;

    @Value("classpath:knowledge-base/airspace_application_process.txt")
    private Resource airspaceProcessResource;

    private List<String> knowledgeBase = new ArrayList<>();

    private static final String SYSTEM_PROMPT = """
        你是无人机租赁平台的智能助手，专门为用户提供无人机租赁相关的咨询服务。
        
        你需要基于以下知识库内容回答用户问题：
        1. 无人机租赁规则 - 包括租赁流程、费用标准、保险赔偿等
        2. 民航局飞行规定 - 包括飞行限制、高度限制、操作资质等
        3. 无人机保险条例 - 包括保险类型、费用、理赔流程等
        4. 空域申请流程 - 包括申请条件、流程、审批标准等
        
        回答要求：
        - 只回答与无人机租赁相关的问题
        - 回答要准确、专业、友好
        - 如果问题超出知识库范围，请引导用户联系客服
        - 回答时引用相关规则条款
        """;

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    public void loadKnowledgeBase() {
        try {
            knowledgeBase.clear();
            knowledgeBase.add(loadResource(rentalRulesResource));
            knowledgeBase.add(loadResource(aviationRegulationsResource));
            knowledgeBase.add(loadResource(insuranceRegulationsResource));
            knowledgeBase.add(loadResource(airspaceProcessResource));
            log.info("知识库加载完成，共加载 {} 个文档", knowledgeBase.size());
        } catch (Exception e) {
            log.error("知识库加载失败", e);
        }
    }

    private String loadResource(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String chat(String userMessage) {
        String context = findRelevantContext(userMessage);
        
        String enhancedPrompt = String.format("""
            上下文知识：
            %s
            
            用户问题：%s
            
            请基于上下文知识回答用户问题，如果上下文中没有相关信息，请说明并引导用户联系客服。
            """, context, userMessage);
        
        Prompt prompt = new Prompt(List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(enhancedPrompt)
        ));
        
        return chatClient.prompt()
            .messages(prompt.getInstructions())
            .call()
            .content();
    }

    private String findRelevantContext(String query) {
        StringBuilder sb = new StringBuilder();
        for (String doc : knowledgeBase) {
            if (containsKeywords(doc, query)) {
                sb.append(doc.substring(0, Math.min(2000, doc.length()))).append("\n\n---\n\n");
            }
        }
        return sb.toString();
    }

    private boolean containsKeywords(String doc, String query) {
        String[] keywords = query.toLowerCase().split("\\s+");
        for (String keyword : keywords) {
            if (keyword.length() > 2 && doc.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public List<String> searchKnowledge(String query) {
        List<String> results = new ArrayList<>();
        for (String doc : knowledgeBase) {
            if (containsKeywords(doc, query)) {
                results.add(doc.substring(0, Math.min(500, doc.length())));
            }
        }
        return results;
    }
}