package com.ai.ai_research_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ai.ai_research_agent.handler.FloatArrayTypeHandler;
import lombok.Data;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;

@Data
public class VectorKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;

    @TableField(typeHandler = FloatArrayTypeHandler.class)
    private float[] embedding;

    private String source;

    /** 文档ID（PDF文件名或唯一标识） */
    private String docId;
    /** 文档标题 */
    private String docTitle;
    /** 页码（从1开始） */
    private Integer pageNum;
    /** 分块类型：text/table/image */
    private String chunkType;
    /** 上下文说明（如"位于第3页，标题为'技术架构'"） */
    private String context;

    private LocalDateTime createTime;

    public static VectorKnowledge fromDocument(Document doc, float[] embedding, String source) {
        VectorKnowledge vk = new VectorKnowledge();
        vk.setContent(doc.getText());
        vk.setEmbedding(embedding);
        vk.setSource(source);

        // 从 Document metadata 中提取 PDF 元数据
        vk.setDocId((String) doc.getMetadata().get("doc_id"));
        vk.setDocTitle((String) doc.getMetadata().get("doc_title"));
        vk.setPageNum((Integer) doc.getMetadata().get("page_num"));
        vk.setChunkType((String) doc.getMetadata().get("chunk_type"));
        vk.setContext((String) doc.getMetadata().get("context"));
        return vk;
    }
}