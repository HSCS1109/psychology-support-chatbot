package org.example.aispringboot.config;

import org.example.aispringboot.AiService.PsychologyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    // 聊天客户端：注册心理咨询领域工具（量表查询、热线查询）
    // 记忆体使用 SummaryChatMemory（摘要压缩），由 Spring 自动注入
    @Bean("open-ai")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel,
                                       PsychologyTools tools,
                                       ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                // 显式指定模型，避免自动配置默认模型名在硅基流动不存在
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-ai/DeepSeek-V3")
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .defaultSystem("你是一个专业的心理疏导师，温和耐心，善于倾听，能够提供专业的心理支持和建议").build();
    }
}
