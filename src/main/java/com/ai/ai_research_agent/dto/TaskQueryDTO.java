package com.ai.ai_research_agent.dto;

import lombok.Data;

/**
 * 任务列表查询 请求入参
 */
@Data
public class TaskQueryDTO {

    /**
     * 按状态查询：WAIT/RUN/SUCCESS/FAIL（可选）
     */
    private String status;

    /**
     * 页码（默认第一页）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10条）
     */
    private Integer pageSize = 10;
}
