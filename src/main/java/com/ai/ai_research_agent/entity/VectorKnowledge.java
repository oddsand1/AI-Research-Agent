package com.ai.ai_research_agent.entity;
import org.springframework.ai.document.Document;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VectorKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private float[] embedding;
    private LocalDateTime createTime;

    //工具方法：Document 转 实体 （适配Spring AI）
    public static VectorKnowledge fromDocument(Document doc,float[] embedding){
        VectorKnowledge vk = new VectorKnowledge();
        vk.setContent(doc.getContent());
        vk.setEmbedding(embedding);
        return vk;
    }
}
