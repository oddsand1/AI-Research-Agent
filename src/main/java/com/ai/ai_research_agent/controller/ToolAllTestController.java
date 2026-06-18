package com.ai.ai_research_agent.controller;


import com.ai.ai_research_agent.common.Result;
import com.ai.ai_research_agent.tool.DbQueryTool;
import com.ai.ai_research_agent.tool.JsonFormatTool;
import com.ai.ai_research_agent.tool.TextSummaryTool;
import com.ai.ai_research_agent.tool.WebCrawlerTool;
import com.ai.ai_research_agent.tool.WebSearchTool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tool/all")
public class ToolAllTestController {

    private final WebSearchTool webSearchTool;
    private final WebCrawlerTool webCrawlerTool;
    private final DbQueryTool dbQueryTool;
    private final TextSummaryTool textSummaryTool;
    private final JsonFormatTool jsonFormatTool;

    public ToolAllTestController(WebSearchTool webSearchTool,
                                 WebCrawlerTool webCrawlerTool,
                                 DbQueryTool dbQueryTool,
                                 TextSummaryTool textSummaryTool,
                                 JsonFormatTool jsonFormatTool) {
        this.webSearchTool = webSearchTool;
        this.webCrawlerTool = webCrawlerTool;
        this.dbQueryTool = dbQueryTool;
        this.textSummaryTool = textSummaryTool;
        this.jsonFormatTool = jsonFormatTool;
    }

    @GetMapping("/search")
    public Result<String> search(@RequestParam String keyword) {
        return Result.success(webSearchTool.webSearch(keyword));
    }

    @GetMapping("/crawler")
    public Result<String> crawler(@RequestParam String url) {
        return Result.success(webCrawlerTool.crawlPage(url));
    }

    @GetMapping("/db")
    public Result<String> db(@RequestParam String type, @RequestParam(required = false) String keyword) {
        return Result.success(dbQueryTool.dbQuery(type, keyword));
    }

    @GetMapping("/summary")
    public Result<String> summary(@RequestParam String content) {
        return Result.success(textSummaryTool.summary(content));
    }

    @GetMapping("/json")
    public Result<String> json(@RequestParam String content) {
        return Result.success(jsonFormatTool.formatJson(content));
    }
}
