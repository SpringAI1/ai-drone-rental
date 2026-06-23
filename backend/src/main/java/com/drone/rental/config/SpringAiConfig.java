package com.drone.rental.config;

import com.drone.rental.ai.rag.RagService;
import com.drone.rental.ai.tools.DomainOperationTools;
import com.drone.rental.ai.tools.DroneQueryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SpringAiConfig {

    /**
     * 默认 ChatClient：携带默认系统提示 + 业务工具
     * （RAG + Memory advisor 在 DroneChatService 里按需注入）
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel,
                                 DroneQueryTools droneQueryTools,
                                 DomainOperationTools domainOperationTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是「翱翔无人机租赁平台」的 AI 智能客服"小飞"。
                        专精民用无人机租赁业务，熟悉中国民航局法规。
                        回答简洁专业，不超过 200 字。
                        涉及业务数据时主动调用工具查询真实数据。
                        """)
                .defaultTools(droneQueryTools, domainOperationTools)
                .build();
    }
}
