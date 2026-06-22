package com.ai.ai_research_agent.rag;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 检索结果：带可追溯引用元数据
 */
@Data
@AllArgsConstructor
public class RagResult {

    /** 内容 */
    private String content;
    /** 文档标题 */
    private String docTitle;
    /** 页码 */
    private Integer pageNum;
    /** 分块类型 */
    private String chunkType;
    /** 上下文说明 */
    private String context;

    public String toCitation() {
        if (docTitle == null) return content;
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        sb.append("\n[来源：《").append(docTitle).append("》");
        if (pageNum != null) sb.append(" 第").append(pageNum).append("页");
        if (context != null) sb.append(" ").append(context);
        sb.append("]");
        return sb.toString();
    }
}
