package com.ai.ai_research_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RAG文档入库 请求入参
 */
@Data
public class RagStoreDTO {

    @NotBlank(message = "文档内容不能为空")
    private String content;

    /**
     * 可选：文档来源/分类，扩展用
     */
    private String source;
}
