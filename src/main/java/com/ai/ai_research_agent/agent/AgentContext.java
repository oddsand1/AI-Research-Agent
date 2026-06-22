package com.ai.ai_research_agent.agent;

import lombok.Data;

/**
 * 智能体全局上下文
 * 跨智能体数据流转、任务状态、中间结果存储
 */
@Data
public class AgentContext {
    /** 任务ID，关联数据库任务表 */
    private Long taskId;
    /** 用户原始提问 */
    private String userQuery;
    /** 搜索/抓取得到的原始信息 */
    private String rawSearchContent;
    /** RAG 检索得到的历史知识库上下文 */
    private String ragContext;
    /** 分析后的精简内容 */
    private String analyzedContent;
    /** 最终报告内容 */
    private String finalReport;
    /** 任务状态：WAIT/RUN/SUCCESS/FAIL */
    private String taskStatus;
    /** PDF 文档ID（精确读页/抽表时使用） */
    private String docId;
    /** PDF 页码（精确读页/抽表时使用） */
    private Integer pageNum;
}