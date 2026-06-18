package com.ai.ai_research_agent.vo;


import lombok.Data;

import java.util.List;

/**
 * 任务列表响应 VO
 */
@Data
public class TaskListVO {

    /**
     * 任务列表
     */
    private List<TaskResponseVO> tasks;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;
}
