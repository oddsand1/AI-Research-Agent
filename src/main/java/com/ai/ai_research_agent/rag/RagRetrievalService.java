package com.ai.ai_research_agent.rag;


import com.ai.ai_research_agent.entity.VectorKnowledge;
import com.ai.ai_research_agent.mapper.VectorKnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        List<String> chunks = textSplitter.split(rawText);
        if(CollectionUtils.isEmpty(chunks)){
            return;
        }
        List<float[]> embeddingList = embeddingService.batchEmbedding(chunks);
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingList.get(i);
            VectorKnowledge knowledge = VectorKnowledge.fromDocument(new Document(chunk), embedding, source);
            vectorKnowledgeMapper.insert(knowledge);
        }
    }

    /**
     * 2. 混合检索：Query扩写 → 去重 -> 向量+关键词多路召回  → RRF融合
     */
    public String retrieveContext(String query, int topK) {
        // 1. Query 扩写
        List<String> queries = expandQuery(query);
        queries = new ArrayList<>(new LinkedHashSet<>(queries));// 精确字符串去重
        // 2. 多查询并行检索，收集全部结果
        List<Document> vectorResults = new ArrayList<>();
        List<Document> keywordResults = new ArrayList<>();
        for (String q : queries) {
            vectorResults.addAll(vectorSearch(q, topK));
            keywordResults.addAll(keywordSearch(q, topK));
        }
        // 3. RRF融合
        List<String> merged = rrfMerge(vectorResults, keywordResults, topK);
        log.info("RRF融合后输出：{} 条", merged.size());
        if (merged.isEmpty()) {
            return "";
        }
        return String.join("\n---\n", merged);
    }

    /**
     * 向量检索：查 vector_knowledge 表，pgvector 余弦相似度
     */
    private List<Document> vectorSearch(String query, int topK) {
        float[] qEmbedding = embeddingService.embed(query);
        String vecStr = toVectorString(qEmbedding);
        return vectorKnowledgeMapper.vectorSearch(vecStr, 0.5, topK).stream()
                .map(vk -> new Document(vk.getContent()))
                .collect(Collectors.toList());
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
    private List<Document> keywordSearch(String query, int limit) {
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
        return vectorKnowledgeMapper.selectList(wrapper).stream()
                .map(vk -> new Document(vk.getContent()))
                .collect(Collectors.toList());
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
    private List<String> rrfMerge(List<Document> vectorDocs, List<Document> keywordDocs, int topK) {
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        double k = 60;

        for (int i = 0; i < vectorDocs.size(); i++) {
            scoreMap.merge(vectorDocs.get(i).getText().trim(), 1.0 / (k + i + 1), Double::sum);
        }
        for (int i = 0; i < keywordDocs.size(); i++) {
            scoreMap.merge(keywordDocs.get(i).getText().trim(), 1.0 / (k + i + 1), Double::sum);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }






    /**
     *  3.解析 PDF 文件，提取纯文本内容
     */
    public String extractPdfContent(MultipartFile file) {
        try(PDDocument document= Loader.loadPDF(file.getBytes())){
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text=stripper.getText(document);
            log.info("PDF解析完成，页数：{}，文本长度：{}", document.getNumberOfPages(), text.length());
            return text;
        }catch (IOException e){
            throw new IllegalArgumentException("PDF解析失败:"+e.getMessage(),e);
        }
    }
}