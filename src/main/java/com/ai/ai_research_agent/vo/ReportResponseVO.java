package com.ai.ai_research_agent.vo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报告详情响应 VO
 */
@Data
public class ReportResponseVO {

    /**
     * 报告ID
     */
    private Long id;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 报告内容
     */
    private String content;

    /**
     * 生成时间
     */
    private LocalDateTime createTime;
}
