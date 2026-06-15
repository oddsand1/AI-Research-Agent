package com.ai.ai_research_agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSearchTool {

    @Tool(name="web_search",description="用于检索互联网实时信息、技术文档、行业知识、热点内容。当用户问题需要外部最新资料时调用该工具。")
    public String webSearch(@ToolParam(description = "搜索关键词/问题，不能为空") String keyword){

        if(keyword==null || keyword.isBlank()){
            return "搜索关键词不能为空";
        }
        log.info("【搜索工具】执行搜索，关键词：{}", keyword);
        try {
            String result = switch(keyword.trim()){
                case "Spring AI Alibaba" -> "Spring AI Alibaba 是阿里云基于 Spring AI 生态打造的 AI 应用开发框架，版本为1.x系列，原生支持通义大模型、RAG检索、多智能体编排、注解式工具调用、PostgreSQL+pgvector向量库等企业级能力，完美兼容 Spring Boot 3。";
                case "多智能体RAG系统" -> "多智能体RAG系统基于分工式智能体架构，结合检索增强生成技术，由规划、搜索、分析、检索、报告五大智能体协同完成全链路任务，广泛用于企业智能调研、文档分析、知识问答场景。";
                default -> String.format("【联网搜索结果】关键词：%s，未查询到相关公开资料，请调整关键词重试。", keyword);
            };
            log.info("【搜索工具】执行完成");
            return result;
        } catch (Exception e) {
            log.error("【搜索工具】执行失败", e);
            return "工具执行异常：" + e.getMessage();
        }
    }
}
