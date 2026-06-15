package com.ai.ai_research_agent.tool;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具基础配置类
 * 将 SAA 的 DashScopeChatModel 包装为 SA 的 ChatClient，供工具类注入使用
 */
@Configuration
public class ToolConfig {

    @Bean
    public ChatClient chatClient(DashScopeChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
