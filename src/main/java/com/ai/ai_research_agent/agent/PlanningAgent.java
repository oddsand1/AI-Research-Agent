package com.ai.ai_research_agent.agent;


import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规划智能体：全局任务调度中心、需求拆解、流程决策
 * 加载全量工具，ReAct 自主决策执行链路
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanningAgent {

    // 注入所需依赖
    private final ChatModel chatModel;
    private final List<ToolCallback> allToolCallbacks;
    private final SearchAgent searchAgent;
    private final RAGAgent ragAgent;
    private final AnalyzeAgent analyzeAgent;
    private final ReportAgent reportAgent;

    // reactAgent 延迟初始化
    private ReactAgent reactAgent;

    // 在依赖注入完成后初始化 reactAgent
    @PostConstruct
    private void init() {
        this.reactAgent = ReactAgent.builder()
                .name("planning_agent")
                .description("全局任务调度中心，负责需求拆解与流程决策")
                .instruction("你是任务规划专家，根据用户问题判断执行策略。仅输出 RAG_FIRST 或 SEARCH_FIRST。RAG_FIRST代表优先查历史知识库，SEARCH_FIRST代表优先联网搜索。")
                .model(chatModel)
                .tools(allToolCallbacks)
                .build();
        log.info("PlanningAgent 初始化完成");
    }

    public AgentContext planTask(String userQuery,Long taskId){
        log.info("【规划智能体】开始处理任务ID：{}，用户提问：{}", taskId, userQuery);
        AgentContext context=new AgentContext();
        context.setTaskId(taskId);
        context.setUserQuery(userQuery);
        context.setTaskStatus("RUN");

        try{
            //决策：判断执行顺序
            String decision=reactAgent.call(userQuery).getText().trim();
            log.info("【规划智能体】执行决策：{}", decision);

            // 串行调度子智能体
            if("RAG_FIRST".equalsIgnoreCase(decision)){
                context=ragAgent.retrieve(context);
            }
            // 其他决策：SEARCH_FIRST
            context=searchAgent.search(context);
            context=analyzeAgent.analyze(context);
            context=reportAgent.generateReport(context);

            context.setTaskStatus("SUCCESS");
            log.info("【规划智能体】任务{}执行完成", taskId);
        }catch (Exception e){
            context.setTaskStatus("FAIL");
            log.error("【规划智能体】任务{}执行异常", taskId, e);
        }
        return context;
    }

}



