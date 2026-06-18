package com.ai.ai_research_agent.dto;

import lombok.Data;

/**
 * 报告列表查询 请求入参
 */
@Data
public class ReportQueryDTO {

    /**
     * 按任务ID查询（可选）
     */
    private Long taskId;

    /**
     * 关键词搜索（可选）
     */
    private String keyword;

    /**
     * 页码（默认第一页）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10条）
     */
    private Integer pageSize = 10;
}
