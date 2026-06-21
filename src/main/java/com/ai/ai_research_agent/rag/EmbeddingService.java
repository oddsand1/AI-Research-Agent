package com.ai.ai_research_agent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;


//生成向量服务

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    //自动注入yml里配置的embedding模型（text-embedding-v3）
    private final EmbeddingModel embeddingModel;


    /**
     * 单个文本生成向量
     */
    public float[] embed(String content) {
        return embeddingModel.embed(content);
    }

    /**
     * 批量文本生成向量
     */
    public List<float[]> batchEmbedding(List<String> textList) {
        return embeddingModel.embed(textList);
    }

}
