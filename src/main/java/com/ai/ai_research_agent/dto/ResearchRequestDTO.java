package com.ai.ai_research_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * 研究任务请求入参
 */
@Data
public class ResearchRequestDTO {
    @NotBlank(message = "用户查询不能为空")  
    private String userQuery;


    /**
     * 是否跳过RAG检索（可选，默认false）
     */
    private Boolean skipRAG = false;
}
