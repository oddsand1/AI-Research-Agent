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

    // 添加类型处理器注解
    @TableField(typeHandler = FloatArrayTypeHandler.class)
    private float[] embedding;

    private String source;

    private LocalDateTime createTime;

    public static VectorKnowledge fromDocument(Document doc, float[] embedding, String source){
        VectorKnowledge vk = new VectorKnowledge();
        vk.setContent(doc.getText());
        vk.setEmbedding(embedding);
        vk.setSource(source);
        return vk;
    }
}