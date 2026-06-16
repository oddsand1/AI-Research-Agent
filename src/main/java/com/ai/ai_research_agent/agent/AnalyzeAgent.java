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
@Component
@RequiredArgsConstructor
public class AnalyzeAgent {

    private final ChatModel chatModel;
    private final List<ToolCallback> processToolCallbacks;

    private ReactAgent reactAgent;

    @PostConstruct
    public void init(){
        this.reactAgent=reactAgent.builder()
                .name("analyze_agent")
                .description("内容分析智能体，负责摘要提炼、JSON格式化和数据库查询")
                .instruction("你是内容分析专家，对多源信息去重、过滤无效内容、提炼核心要点，输出规整的分析结果，可使用摘要工具优化文本。")
                .model(chatModel)
                .tools(processToolCallbacks)
                .build();
        log.info("AnalyzeAgent 初始化完成");
    }


    public AgentContext analyze(AgentContext context){
        log.info("【分析智能体】开始分析，任务ID：{}", context.getTaskId());
        try{
            StringBuilder prompt=new StringBuilder("请分析以下信息：\n");
            if(context.getRawSearchContent()!=null && !context.getRawSearchContent().isBlank()){
                prompt.append("【搜索结果】\n").append(context.getRawSearchContent()).append("\n\n");
            }
            if (context.getRagContext() != null && !context.getRagContext().isBlank()) {
                prompt.append("【历史知识库】\n").append(context.getRagContext()).append("\n\n");
            }
            prompt.append("请对以上信息进行摘要提炼和结构化分析。");

            String result=reactAgent.call(prompt.toString()).getText().trim();
            context.setAnalyzedContent(result);
            log.info("【分析智能体】分析完成，结果长度：{}", result.length());
        } catch (Exception e) {
            log.error("【分析智能体】分析异常", e);
            context.setAnalyzedContent("分析失败：" + e.getMessage());
        }
       return context;
    }
}
