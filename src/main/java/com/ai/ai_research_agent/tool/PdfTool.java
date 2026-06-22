package com.ai.ai_research_agent.tool;


import com.ai.ai_research_agent.entity.PdfChunk;
import com.ai.ai_research_agent.mapper.PdfChunkMapper;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PDF 检索工具：Agent 按需调用
 * - searchPdf: 语义检索 PDF 相关片段
 * - readPage: 精确读取指定页
 * - extractTable: 提取结构化表格
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTool {

    private final RagRetrievalService ragRetrievalService;
    private final PdfChunkMapper pdfChunkMapper;

    /**
     * 在 PDF 知识库中语义检索相关片段
     */
    public String searchPdf(String query, int topK) {
        log.info("【PdfTool】语义检索：{}", query);
        String context = ragRetrievalService.retrieveContext(query, topK);
        if (context.isEmpty()) {
            return "未检索到相关 PDF 内容";
        }
        return "【PDF 检索结果】\n" + context;
    }


    /**
     * 精确读取指定文档的指定页
     */
    public String readPage(String docId, int pageNum) {
        log.info("【PdfTool】读取页面：doc={}, page={}", docId, pageNum);
        List<PdfChunk> chunks = pdfChunkMapper.selectByPage(docId, pageNum);
        if (chunks.isEmpty()) {
            return "未找到文档《" + docId + "》第" + pageNum + "页";
        }
        return chunks.stream()
                .map(c -> "【" + c.getDocTitle() + " 第" + c.getPageNum() + "页"
                        + (c.getSectionTitle() != null ? " " + c.getSectionTitle() : "")
                        + "】\n" + c.getContent())
                .collect(Collectors.joining("\n---\n"));
    }


    /**
     * 提取指定页的结构化表格
     */
    public String extractTable(String docId,int pageNum){
        log.info("【PdfTool】提取表格：doc={}, page={}", docId, pageNum);
        List<PdfChunk> tables=pdfChunkMapper.selectByType(docId, "table");
        List<PdfChunk> pageTables=tables.stream()
                .filter(c-> c.getPageNum() != null && c.getPageNum() == pageNum)
                .toList();
        if(pageTables.isEmpty()){
            return "文档《" + docId + "》第" + pageNum + "页未检测到表格";
        }
        return pageTables.stream()
                .map(t -> "【表格 第" + t.getPageNum() + "页】\n"
                        + (t.getTableData() != null ? t.getTableData() : t.getContent()))
                .collect(Collectors.joining("\n\n"));
    }


    /**
     * 列出所有已入库的 PDF 文档
     */
    public String listDocs(){
        log.info("【PdfTool】列出所有文档");
        List<PdfChunk> chunks=pdfChunkMapper.selectList(null);
        if(chunks.isEmpty()){
            return "知识库中暂无 PDF 文档";
        }
        return chunks.stream()
                .map(PdfChunk::getDocId)
                .distinct()
                .collect(Collectors.joining("\n", "已入库文档：\n", ""));
    }


    /**
     * 查看指定页的图片/图表描述
     */
    public String analyzeImage(String docId, int pageNum) {
        log.info("【PdfTool】分析图片：doc={}, page={}", docId, pageNum);
        List<PdfChunk> images = pdfChunkMapper.selectByType(docId, "image");
        List<PdfChunk> pageImages = images.stream()
                .filter(c -> c.getPageNum() != null && c.getPageNum() == pageNum)
                .toList();
        if (pageImages.isEmpty()) {
            return "文档《" + docId + "》第" + pageNum + "页未检测到图片/图表";
        }
        return pageImages.stream()
                .map(img -> "【图片 第" + img.getPageNum() + "页】\n"
                        + (img.getImageCaption() != null ? img.getImageCaption() : img.getContent()))
                .collect(Collectors.joining("\n\n"));
    }
}