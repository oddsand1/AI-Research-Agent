package com.ai.ai_research_agent.tool;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


// 工具回调注册 + 工具分组（供Agent调用）

/**
 * 新版 Spring AI 1.x 工具回调注册中心
 * 将 @Tool 注解工具转为框架识别的 ToolCallback，并按业务分组
 * 不同智能体按需注入对应分组工具，企业级最佳实践
 */
@RequiredArgsConstructor
@Configuration
public class ToolCallbackConfig {

    private final WebSearchTool webSearchTool;
    private final WebCrawlerTool webCrawlerTool;
    private final JsonFormatTool jsonFormatTool;
    private final DbQueryTool dbQueryTool;
    private final TextSummaryTool textSummaryTool;



    /**
     * 全量工具集合：规划智能体使用（总调度，需要所有工具）
     */
    @Bean
    public ToolCallback[] allToolCallbacks() {
        return ToolCallbacks.from(
                webSearchTool,
                webCrawlerTool,
                dbQueryTool,
                textSummaryTool,
                jsonFormatTool
        );
    }

    /**
     * 搜索类工具集合：搜索智能体专用
     */
    @Bean
    public ToolCallback[] searchToolCallbacks() {
        return ToolCallbacks.from(webSearchTool, webCrawlerTool);
    }

    /**
     * 数据处理类工具集合：分析/检索/报告智能体专用
     */
    @Bean
    public ToolCallback[] processToolCallbacks() {
        return ToolCallbacks.from(dbQueryTool, textSummaryTool, jsonFormatTool);
    }
}
