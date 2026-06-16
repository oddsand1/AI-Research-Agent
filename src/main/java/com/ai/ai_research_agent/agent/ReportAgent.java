package com.ai.ai_research_agent.agent;


import com.ai.ai_research_agent.entity.ResearchReport;
import com.ai.ai_research_agent.mapper.ResearchReportMapper;
import com.ai.ai_research_agent.mapper.ResearchTaskMapper;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportAgent {

    private final List<ToolCallback> processToolCallbacks;
    private final ChatModel chatModel;
    private final ResearchReportMapper researchReportMapper;
    private final ResearchTaskMapper researchTaskMapper;
    private final RagRetrievalService ragRetrievalService;

    private ReactAgent reactAgent;

    @PostConstruct
    public void init(){
        this.reactAgent=reactAgent.builder()
                .name("ReportAgent")
                .description("报告生成智能体，负责将分析结果转为结构化报告")
                .instruction("你是专业报告撰写者，基于素材生成完整通顺的研究报告，可使用JSON格式化工具输出结构化内容。")
                .model(chatModel)
                .tools(processToolCallbacks)
                .build();
        log.info("ReportAgent 初始化完成");
    }


    public AgentContext generateReport(AgentContext context){
        log.info("【报告智能体】开始生成报告，任务ID：{}", context.getTaskId());
        try {
            //1.生成报告
            String prompt = "请基于以下分析内容，生成结构化最终报告：\n" + context.getAnalyzedContent();
            String result = reactAgent.call(prompt).getText().trim();
            context.setFinalReport(result);
            log.info("【报告智能体】报告生成完成，长度：{}", result.length());

            //2.报告入库
            ResearchReport report=new ResearchReport();
            report.setTaskId(context.getTaskId());
            report.setContent(result);
            report.setCreateTime(LocalDateTime.now());
            researchReportMapper.insert(report);
            log.info("【报告智能体】报告已入库，报告ID：{}", report.getId());

            //3.报告向量化入库 （向量库，供后续RAG检索）
            ragRetrievalService.storeDocument(result);
            log.info("【报告智能体】报告已向量化入库");
        } catch (Exception e) {
            log.error("【报告智能体】报告生成异常", e);
            context.setFinalReport("报告生成失败：" + e.getMessage());
        }
        return context;
    }
}
