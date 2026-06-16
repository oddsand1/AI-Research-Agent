package com.ai.ai_research_agent.agent;


import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class SearchAgent {

    private final ChatModel chatModel;
    private final List<ToolCallback> searchToolCallbacks;

    private ReactAgent reactAgent;


    @PostConstruct
    public void init() {
        this.reactAgent=ReactAgent.builder()
                .name("search_agent")
                .description("联网搜索智能体，调用搜索和爬虫工具获取最新信息")
                .instruction("你是专业信息检索员，使用搜索工具查找信息，遇到网页链接就用抓取工具获取详情，最后整合所有内容返回。")
                .model(chatModel)
                .tools(searchToolCallbacks)
                .build();
        log.info("SearchAgent 初始化完成");
    }


    public AgentContext search(AgentContext context) {
        log.info("【搜索智能体】开始搜索，任务ID：{}", context.getTaskId());
        try{
            String prompt = "请搜索以下问题并整理结果：\n" + context.getUserQuery();
            String result=reactAgent.call(prompt).getText().trim();
            context.setRawSearchContent(result);
            log.info("【搜索智能体】搜索完成，结果长度：{}", result.length());
        }catch (Exception e){
            log.error("【搜索智能体】搜索异常", e);
            context.setRawSearchContent("搜索失败：" + e.getMessage());
        }
        return context;
    }
}
