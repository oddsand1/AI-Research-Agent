package com.ai.ai_research_agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务详情响应 VO
 */
@Data
public class TaskResponseVO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 用户提问
     */
    private String userQuery;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}