package com.ai.ai_research_agent.agent;

import com.ai.ai_research_agent.context.AgentContext;
import com.ai.ai_research_agent.tool.WebCrawlerTool;
import com.ai.ai_research_agent.tool.WebSearchTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SearchAgent {

    private final ChatModel chatModel;
    private final WebSearchTool webSearchTool;
    private final WebCrawlerTool webCrawlerTool;

    private static final int MAX_ROUNDS = 2;

    public AgentContext search(AgentContext context) {
        log.info("【搜索智能体】开始搜索，任务ID：{}", context.getTaskId());
        try {
            // 1. LLM 提炼精准搜索关键词
            String keywords = refineKeywords(context.getUserQuery());
            log.info("【搜索智能体】关键词提炼：{} → {}", context.getUserQuery(), keywords);

            // 2. 多轮搜索
            StringBuilder buffer = new StringBuilder();
            String currentKw = keywords;

            for (int round = 1; round <= MAX_ROUNDS; round++) {
                log.info("【搜索智能体】第{}轮搜索，关键词：{}", round, currentKw);

                String searchResult = webSearchTool.webSearch(currentKw);
                buffer.append("【第").append(round).append("轮搜索】\n").append(searchResult).append("\n\n");

                // 抓取搜索结果中的 URL
                String crawled = crawlUrls(searchResult);
                if (!crawled.isEmpty()) {
                    buffer.append("【网页抓取】\n").append(crawled).append("\n\n");
                }

                // 判断是否需要补充搜索
                if (round < MAX_ROUNDS && isSufficient(buffer.toString())) {
                    log.info("【搜索智能体】信息充分，停止搜索");
                    break;
                }
                if (round < MAX_ROUNDS) {
                    currentKw = expandKeywords(context.getUserQuery(), currentKw, buffer.toString());
                    log.info("【搜索智能体】扩词补充：{}", currentKw);
                }
            }

            // 3. LLM 整合摘要
            String summary = summarize(buffer.toString(), context.getUserQuery());
            context.setRawSearchContent(summary);
            log.info("【搜索智能体】搜索完成，摘要长度：{}", summary.length());

        } catch (Exception e) {
            log.error("【搜索智能体】搜索异常", e);
            context.setRawSearchContent("搜索失败：" + e.getMessage());
        }
        return context;
    }

    /**
     * 关键词提炼：把自然语言问题转为精准搜索词
     */
    private String refineKeywords(String userQuery) {
        String prompt = "将以下问题提炼为3-5个精准搜索关键词（空格分隔，只输出关键词）：\n" + userQuery;
        return chatModel.call(prompt).trim();
    }

    /**
     * 判断搜索结果是否充分
     */
    private boolean isSufficient(String results) {
        if (results.length() < 100) {
            return false;
        }
        String prompt = "以下搜索结果是否包含足够信息回答用户问题？仅回复 YES 或 NO：\n" + results;
        return "YES".equalsIgnoreCase(chatModel.call(prompt).trim());
    }

    /**
     * 信息不足时，扩词生成新搜索词
     */
    private String expandKeywords(String query, String prevKw, String results) {
        String prompt = String.format("""
            原问题：%s
            已用关键词：%s
            已获结果：%s
            信息不足，请生成3-5个新的补充搜索关键词（空格分隔，只输出关键词）：
            """, query, prevKw, results);
        return chatModel.call(prompt).trim();
    }

    /**
     * 整合多轮搜索结果，输出结构化摘要
     */
    private String summarize(String rawResults, String query) {
        String prompt = String.format("""
            请整合以下多轮搜索结果，围绕"%s"输出结构化摘要：
            1. 核心发现（分点列出）
            2. 关键数据/事实
            3. 信息缺口（如存在）

            【搜索结果】
            %s
            """, query, rawResults);
        return chatModel.call(prompt).trim();
    }

    /**
     * 从搜索结果中提取 URL 并抓取网页
     */
    private String crawlUrls(String searchResult) {
        StringBuilder crawled = new StringBuilder();
        String[] lines = searchResult.split("\n");
        for (String line : lines) {
            if (line.startsWith("http://") || line.startsWith("https://")) {
                String url = line.trim().split("\\s")[0];
                String content = webCrawlerTool.crawlPage(url);
                crawled.append(content).append("\n");
            }
        }
        return crawled.toString();
    }
}