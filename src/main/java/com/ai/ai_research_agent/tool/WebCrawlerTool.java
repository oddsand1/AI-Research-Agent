package com.ai.ai_research_agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
@Component
public class WebCrawlerTool {

    @Tool(
            name = "web_crawler",
            description = "根据传入的网页URL地址，抓取网页正文并自动清洗HTML标签、多余空格与换行，返回纯文本内容。仅接收合法HTTP/HTTPS链接。"
    )
    public String crawlPage(
            @ToolParam(description = "需要抓取的完整网页URL，必须以http/https开头") String url
    ) {
        log.info("【网页抓取工具】开始抓取URL：{}", url);
        try {
            if (!StringUtils.hasText(url) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                log.warn("【网页抓取工具】URL非法：{}", url);
                return "参数错误：请传入合法的HTTP/HTTPS网页链接";
            }
            Document doc = Jsoup.connect(url)
                    .timeout(5000)
                    .userAgent("Mozilla/5.0")
                    .get();
            String pureText = doc.text().replaceAll("\\s+", " ").trim();
            log.info("【网页抓取工具】抓取成功，内容长度：{}字符", pureText.length());
            return "网页抓取内容：" + pureText;
        } catch (IOException e) {
            log.error("【网页抓取工具】抓取失败，URL：{}", url, e);
            return "网页抓取失败，URL：" + url + "，异常原因：" + e.getMessage();
        }
    }
}
