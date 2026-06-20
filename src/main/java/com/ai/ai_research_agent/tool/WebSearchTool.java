package com.ai.ai_research_agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSearchTool {

    @Tool(name = "web_search", description = "通过 DuckDuckGo 检索互联网实时信息")
    public String webSearch(@ToolParam(description = "搜索关键词") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "搜索关键词不能为空";
        }
        log.info("【搜索工具】DuckDuckGo 搜索，关键词：{}", keyword);
        try {
            String url = "https://html.duckduckgo.com/html/?q=" + keyword;
            Document doc = Jsoup.connect(url)
                    .timeout(8000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            Elements results = doc.select(".result");
            if (results.isEmpty()) {
                return "DuckDuckGo 未获取到结果，关键词：" + keyword;
            }

            StringBuilder sb = new StringBuilder();
            int count = Math.min(results.size(), 5);
            for (int i = 0; i < count; i++) {
                Element r = results.get(i);
                String title = r.selectFirst(".result__title") != null
                        ? r.selectFirst(".result__title").text() : "";
                String snippet = r.selectFirst(".result__snippet") != null
                        ? r.selectFirst(".result__snippet").text() : "";
                String link = r.selectFirst(".result__url") != null
                        ? r.selectFirst(".result__url").text().trim() : "";
                sb.append(String.format("%d. %s\n   %s\n   %s\n\n", i + 1, title, snippet, link));
            }
            log.info("【搜索工具】完成，结果数：{}", count);
            return sb.toString();
        } catch (Exception e) {
            log.error("【搜索工具】执行失败", e);
            return "搜索异常：" + e.getMessage();
        }
    }
}