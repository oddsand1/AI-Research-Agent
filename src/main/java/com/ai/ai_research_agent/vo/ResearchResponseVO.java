package com.ai.ai_research_agent.vo;

import lombok.Data;

/**
 * 研究任务响应 VO
 */
@Data
public class ResearchResponseVO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务状态：RUN/SUCCESS/FAIL
     */
    private String status;

    /**
     * 最终报告内容
     */
    private String report;

    /**
     * 提示信息
     */
    private String message;
}
