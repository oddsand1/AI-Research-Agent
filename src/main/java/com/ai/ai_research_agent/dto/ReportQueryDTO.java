package com.ai.ai_research_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报告列表查询 请求入参
 */
@Data
public class ReportQueryDTO {

    /**
     * 按任务ID查询（可选）
     */
    @NotBlank(message = "任务ID不能为空")
    private Long taskId;

    /**
     * 关键词搜索（可选）
     */
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    /**
     * 页码（默认第一页）
     */
    @NotBlank(message = "页码不能为空")
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10条）
     */
    @NotBlank(message = "每页数量不能为空")
    private Integer pageSize = 10;

    /**
     * 查询起始时间（可选）
     */
    @NotBlank(message = "查询起始时间不能为空")
    private String startTime;
    /**
     * 查询结束时间（可选）
     */
    @NotBlank(message = "查询结束时间不能为空")
    private String endTime;
}
