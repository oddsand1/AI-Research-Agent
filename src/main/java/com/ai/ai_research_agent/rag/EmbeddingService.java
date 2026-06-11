package com.ai.ai_research_agent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;


//生成向量服务

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;


    /**
     * 单个文本生成向量
     */
    public float[] getEmbedding(String content) {
        return embeddingModel.embed(content);
    }

    /**
     * 批量文本生成向量
     */
    public List<float[]> batchEmbedding(List<String> textList) {
        return embeddingModel.embed(textList);
    }

}
