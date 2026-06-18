package com.ai.ai_research_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagSearchDTO {

    @NotBlank(message = "查询内容不能为空")
    private String query;

    //召回条数，默认3
    private Integer topK=3;
}
