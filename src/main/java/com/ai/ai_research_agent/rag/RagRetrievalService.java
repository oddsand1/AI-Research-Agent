package com.ai.ai_research_agent.rag;


import com.ai.ai_research_agent.entity.VectorKnowledge;
import com.ai.ai_research_agent.mapper.VectorKnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final EmbeddingService embeddingService;
    private final TextSplitter textSplitter;
    private final VectorKnowledgeMapper vectorKnowledgeMapper;
    private final ChatModel chatModel;


    /**
     * 1. 文档入库：文本 → 分片 → 向量化 → 存入PG向量库
     * @param source 来源：upload(手动上传) / report(报告片段)
     */
    public void storeDocument(String rawText, String source) {
        storeDocumentWithMeta(rawText, source, null, null, null, null, null);
    }

    /**
     * 带 PDF 元数据的文档入库
     */
    public void storeDocumentWithMeta(String rawText, String source,
                                      String docId, String docTitle,
                                      Integer pageNum, String chunkType,
                                      String context) {
        List<String> chunks = textSplitter.split(rawText);
        if (CollectionUtils.isEmpty(chunks)) {
            return;
        }
        List<float[]> embeddingList = embeddingService.batchEmbedding(chunks);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingList.get(i);
            VectorKnowledge knowledge = new VectorKnowledge();
            knowledge.setContent(chunk);
            knowledge.setEmbedding(embedding);
            knowledge.setSource(source);
            knowledge.setDocId(docId);
            knowledge.setDocTitle(docTitle);
            knowledge.setPageNum(pageNum);
            knowledge.setChunkType(chunkType);
            knowledge.setContext(context);
            vectorKnowledgeMapper.insert(knowledge);
        }
    }

    /**
     * 2. 混合检索：Query扩写 → 向量+关键词多路召回 → RRF融合 → LLM Rerank → 带引用
     */
    public String retrieveContext(String query, int topK) {
        List<VectorKnowledge> results = retrieveWithMeta(query, topK);
        if (results.isEmpty()) {
            return "";
        }
        return results.stream()
                .map(this::formatCitation)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 带引用元数据的检索（供 PdfTool 和评测使用）
     */
    public List<VectorKnowledge> retrieveWithMeta(String query, int topK) {
        List<String> queries = expandQuery(query);
        queries = new ArrayList<>(new LinkedHashSet<>(queries));

        List<VectorKnowledge> vectorResults = new ArrayList<>();
        List<VectorKnowledge> keywordResults = new ArrayList<>();
        for (String q : queries) {
            vectorResults.addAll(vectorSearch(q, topK));
            keywordResults.addAll(keywordSearch(q, topK));
        }

        List<VectorKnowledge> candidates = rrfMerge(vectorResults, keywordResults, topK * 2);
        log.info("RRF融合候选：{} 条", candidates.size());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<VectorKnowledge> ranked = llmRerank(query, candidates, topK);
        log.info("Rerank后输出：{} 条", ranked.size());
        return ranked;
    }

    private String formatCitation(VectorKnowledge vk) {
        StringBuilder sb = new StringBuilder();
        sb.append(vk.getContent());
        if (vk.getDocTitle() != null) {
            sb.append("\n[来源：《").append(vk.getDocTitle()).append("》");
            if (vk.getPageNum() != null) sb.append(" 第").append(vk.getPageNum()).append("页");
            if (vk.getContext() != null) sb.append(" ").append(vk.getContext());
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * 向量检索：查 vector_knowledge 表，pgvector 余弦相似度
     */
    private List<VectorKnowledge> vectorSearch(String query, int topK) {
        float[] qEmbedding = embeddingService.embed(query);
        String vecStr = toVectorString(qEmbedding);
        return vectorKnowledgeMapper.vectorSearch(vecStr, 0.5, topK);
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }


    /**
     * 关键词检索：PostgreSQL LIKE 模糊匹配，统一转为 Document
     */
    private List<VectorKnowledge> keywordSearch(String query, int limit) {
        LambdaQueryWrapper<VectorKnowledge> wrapper = new LambdaQueryWrapper<>();
        String[] terms = query.split("[\\s，,。.！!？?、；;：:]+");
        wrapper.and(w -> {
            for (int i = 0; i < terms.length; i++) {
                if (terms[i].length() >= 2) {
                    if (i == 0) {
                        w.like(VectorKnowledge::getContent, terms[i]);
                    } else {
                        w.or().like(VectorKnowledge::getContent, terms[i]);
                    }
                }
            }
        });
        wrapper.last("limit " + limit);
        return vectorKnowledgeMapper.selectList(wrapper);
    }


    /**
     *  Query 扩写：LLM 生成多角度查询，提升召回率
     */
    private List<String> expandQuery(String query){
        String prompt="""
        你是检索专家，将用户问题扩展为3个不同角度的查询，提高召回率。
        要求：角度互补、措辞不同、每个不超过30字。
        只输出查询列表，每行一个，不要编号。

        用户问题：%s
        """.formatted(query);

        String raw=chatModel.call(prompt);
        List<String> expanded=new ArrayList<>();
        expanded.add(query);
        for(String line: raw.split("\n")){
            String q=line.replaceAll("^[\\d\\.\\-、]+", "").trim();
            if(!q.isEmpty() && q.length()>=3){
                expanded.add(q);
            }
        }
        log.info("Query扩写：{} → {} 条", query, expanded.size());
        return expanded;
    }


    /**
     * RRF 倒数排名融合算法
     * RRF_score(doc) = Σ 1/(k + rank_i)，k=60
     * 两个列表都已按相关度从高到低排序
     */
    private List<VectorKnowledge> rrfMerge(List<VectorKnowledge> vectorDocs, List<VectorKnowledge> keywordDocs, int topK) {
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        Map<String, VectorKnowledge> docMap = new LinkedHashMap<>();
        double k = 60;

        for (int i = 0; i < vectorDocs.size(); i++) {
            String key = vectorDocs.get(i).getContent().trim();
            scoreMap.merge(key, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(key, vectorDocs.get(i));
        }
        for (int i = 0; i < keywordDocs.size(); i++) {
            String key = keywordDocs.get(i).getContent().trim();
            scoreMap.merge(key, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(key, keywordDocs.get(i));
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> docMap.get(e.getKey()))
                .collect(Collectors.toList());
    }


    /**
     *  LLM Rerank：用 LLM 对候选文档打分重排，取 topK
     */
    private List<VectorKnowledge> llmRerank(String query, List<VectorKnowledge> candidates, int topK) {
        if (candidates.size() <= topK) {
            return candidates;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i, candidates.get(i).getContent()));
        }
        String prompt = String.format("""
            你是检索质量评估专家，请对以下候选文档进行相关性打分。
            【用户问题】%s
            【候选文档】
            %s
            要求：选出与问题最相关的%d条文档，每行输出一个编号（如 3,7,1,5），按相关度从高到低排序。
            只输出编号列表，用逗号分隔。
            """, query, sb.toString(), topK);
        String raw = chatModel.call(prompt).trim();
        List<Integer> order = parseRerankOrder(raw, candidates.size());
        List<VectorKnowledge> result = new ArrayList<>();
        for (int idx : order) {
            if (result.size() >= topK) break;
            if (idx >= 0 && idx < candidates.size()) {
                result.add(candidates.get(idx));
            }
        }
        if (result.isEmpty()) {
            log.warn("Rerank解析失败，使用原始排序");
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
        return result;
    }

    private List<Integer> parseRerankOrder(String raw, int max) {
        List<Integer> order = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            try {
                int idx = Integer.parseInt(token.trim());
                if (idx >= 0 && idx < max && !order.contains(idx)) {
                    order.add(idx);
                }
            } catch (NumberFormatException ignored) {}
        }
        return order;
    }
}