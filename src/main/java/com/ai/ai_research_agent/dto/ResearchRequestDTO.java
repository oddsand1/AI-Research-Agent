package com.ai.ai_research_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 研究任务请求入参
 */
@Data
public class ResearchRequestDTO {

    @NotBlank(message = "调研主题不能为空")
    private String userQuery;

    /**
     * 报告篇幅：short=精简 / long=详细（默认详细）
     */
    private String length = "long";

    /**
     * 是否启用历史知识库参考（默认开启）
     */
    private Boolean enableRAG = true;

    /**
     * 指定模型：deepseek / qwen（默认 deepseek）
     */
    private String model = "deepseek";
}