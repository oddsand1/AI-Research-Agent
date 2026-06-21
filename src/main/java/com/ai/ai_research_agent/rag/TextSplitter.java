package com.ai.ai_research_agent.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义分片器：按标题、段落语义结构切分长文本，保留上下文连贯
 */
@Component
public class TextSplitter {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    /**
     * 基础切分：按固定长度 + 重叠
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chunks;
        }
        int start = 0;
        int textlen = text.length();
        while (start < textlen) {
            int end = Math.min(start + CHUNK_SIZE, textlen);
            chunks.add(text.substring(start, end));
            if (start + CHUNK_SIZE >= textlen) break;
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    /**
     * 语义切分：按 ## 标题拆分章节，每章为一块，过长的按段落再切
     */
    public List<ChunkMeta> splitSemantic(String text) {
        List<ChunkMeta> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chunks;
        }
        // 按 ## 标题拆分
        String[] sections = text.split("\n(?=## )");
        for (String section : sections) {
            if (section.isBlank()) continue;
            String title = extractTitle(section);
            if (section.length() <= CHUNK_SIZE) {
                chunks.add(new ChunkMeta(section, title));
            } else {
                // 过长的章节按段落再切
                String[] paragraphs = section.split("\n\n");
                // 缓冲区，累积段落文本直到达到长度上限
                StringBuilder buf = new StringBuilder();

                for (String para : paragraphs) {
                    // 新增段落会超出阈值 且 缓冲区已有内容：先提交当前块
                    if (buf.length() + para.length() > CHUNK_SIZE && buf.length() > 0) {
                        chunks.add(new ChunkMeta(buf.toString(), title));
                        buf.setLength(0);
                        // 下一块保留当前章节标题作为上下文
                        buf.append("（续）").append(title).append("\n");
                    }
                    buf.append(para).append("\n\n");
                }
                // 循环结束后缓冲区剩余文本，生成最后一个分块
                if (buf.length() > 0) {
                    chunks.add(new ChunkMeta(buf.toString(), title));
                }
            }
        }
        return chunks;
    }

    /**
     * 从章节内容中提取纯标题，去除#标记与前置空格
     * @param section 单章节完整文本（首行为##标题）
     * @return 清洗后的纯章节标题
     */
    private String extractTitle(String section) {
        String firstLine = section.split("\n")[0].trim();
        return firstLine.replaceAll("^#+\\s*", "");
    }


    /**
     * RAG分块元数据记录类
     * @param content 分块原文内容
     * @param sectionTitle 该分块所属章节标题
     */
    public record ChunkMeta(String content, String sectionTitle) {}
}