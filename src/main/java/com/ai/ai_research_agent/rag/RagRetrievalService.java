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
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;
    private final VectorKnowledgeMapper vectorKnowledgeMapper;


    /**
     * 1. 文档入库：文本 → 分片 → 向量化 → 存入PG向量库
     */
    public void storeDocument(String rawText) {
        //文本切片
        List<String> chunks = textSplitter.split(rawText);
        if(CollectionUtils.isEmpty(chunks)){
            return;
        }
        //批量生成向量
        List<float[]> embeddingList = embeddingService.batchEmbedding(chunks);
        //遍历入库
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            float[] embedding = embeddingList.get(i);
            //1.存入自定义业务向量表（mybatis-plus）
            VectorKnowledge knowledge=VectorKnowledge.fromDocument(new Document(chunk),embedding);
            vectorKnowledgeMapper.insert(knowledge);
            //2.存入Spring AI 标准Postgres向量库（用于检索）
            //vectorStore.add方法内部会自动调用EmbeddingModel生成向量
            vectorStore.add(List.of(new Document(chunk)));
        }
    }

    /**
     * 2. 相似度检索：根据用户问题召回相似知识库内容
     *  query 用户提问
     *  topK 召回条数
     * @return 拼接后的上下文文本
     */
    public String retrieveContext(String query, int topK) {
        // 1. 构建检索请求：余弦相似度匹配
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)   // 用户的查询问题
                .topK(topK)   // 返回最相似的 topK 条
                .similarityThreshold(0.7)   // 相似度阈值，低于0.7的结果被过滤
                .build();

        // 2. 执行相似度搜索
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // 3. 处理空结果
        if (CollectionUtils.isEmpty(documents)) {
            return "";
        }
        // 4. 拼接检索结果作为LLM上下文
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }


    /**
     * 解析 PDF 文件，提取纯文本内容
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
