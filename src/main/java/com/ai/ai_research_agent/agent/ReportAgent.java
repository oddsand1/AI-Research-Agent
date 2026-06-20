package com.ai.ai_research_agent.agent;

import com.ai.ai_research_agent.entity.ResearchReport;
import com.ai.ai_research_agent.mapper.ResearchReportMapper;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportAgent {

    private final ChatModel chatModel;
    private final ResearchReportMapper researchReportMapper;
    private final RagRetrievalService ragRetrievalService;

    public AgentContext generateReport(AgentContext context) {
        log.info("【报告智能体】开始生成报告，任务ID：{}", context.getTaskId());
        try {
            // 1. 生成 Markdown 报告
            String prompt = buildPrompt(context);
            String result = chatModel.call(prompt).trim();
            context.setFinalReport(result);
            log.info("【报告智能体】报告生成完成，长度：{}", result.length());

            // 2. 报告入库
            ResearchReport report = new ResearchReport();
            report.setTaskId(context.getTaskId());
            report.setContent(result);
            report.setCreateTime(LocalDateTime.now());
            researchReportMapper.insert(report);
            log.info("【报告智能体】报告已入库，报告ID：{}", report.getId());

            // 3. 按章节拆分，逐段向量化入库
            String[] sections = result.split("\n(?=## )");
            for (String section : sections) {
                if (!section.isBlank()) {
                    ragRetrievalService.storeDocument(section.trim(), "report");
                }
            }
            log.info("【报告智能体】报告已按{}个章节拆分向量化入库", sections.length);

        } catch (Exception e) {
            log.error("【报告智能体】报告生成异常", e);
            context.setFinalReport("报告生成失败：" + e.getMessage());
        }
        return context;
    }

    /**
     * 构建 Markdown 报告生成 Prompt
     */
    private String buildPrompt(AgentContext ctx) {
        return String.format("""
            请基于以下分析内容，生成一份Markdown格式的调研报告，严格遵循以下规范：

            【格式要求】
            1. 使用 ## 二级标题划分章节，### 三级标题划分子节
            2. 报告结构：
               ## 调研背景与目的
               ## 核心发现
               ### 分点一
               ### 分点二
               ...
               ## 关键结论
               ## 参考文献来源
            3. 参考文献部分必须标注所有数据来源和引用出处
            4. 语言专业、客观、简洁，避免主观臆断

            【分析内容】
            %s
            """,
            ctx.getAnalyzedContent()
        );
    }
}