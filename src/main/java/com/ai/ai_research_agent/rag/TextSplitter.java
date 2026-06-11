package com.ai.ai_research_agent.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分片器
 * 功能：将超长文本按照指定长度切割为多个文本片段，支持片段重叠，保证上下文语义连贯
 * 适用场景：RAG知识库、大模型长文本问答等需要拆分长文本的场景
 */
@Component
public class TextSplitter {
    // 单块最大字符数
    private static final int CHUNK_SIZE = 800;
    // 相邻分片之间的重叠字符数，用于保留上下文衔接内容
    private static final int CHUNK_OVERLAP = 100;

    public List<String> split(String text){
        List<String> chunks = new ArrayList<>();

        //传入文本为空，直接返回空集合
        if(!StringUtils.hasText(text)){
            return chunks;
        }

        int start = 0;
        int textlen= text.length();

        while(start < textlen){
            //计算当前分片结束下标，防止下标越界
            int end=Math.min(start+CHUNK_SIZE,textlen);

            //包头不包尾
            String chunk=text.substring(start,end);
            chunks.add(chunk);

            // 修复：当已经到达文本末尾时退出循环
            if (start + CHUNK_SIZE >= textlen) {
                break;
            }
            start=end-CHUNK_OVERLAP;
        }
        return chunks;
    }
}
