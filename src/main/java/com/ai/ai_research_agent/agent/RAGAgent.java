package com.ai.ai_research_agent.agent;

import com.ai.ai_research_agent.rag.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RAGAgent {

    private final RagRetrievalService ragRetrievalService;

    public AgentContext retrieve(AgentContext context) {
        String userQuery = context.getUserQuery();
        log.info("【RAG检索智能体】开始向量检索：{}", userQuery);

        String ragContent=ragRetrievalService.retrieveContext(userQuery,3);
        context.setRagContext(ragContent);

        if (ragContent.isEmpty()) {
            log.warn("【RAG检索智能体】未检索到相关内容：{}", userQuery);
        } else {
            log.info("【RAG检索智能体】检索完成，匹配内容长度：{}", ragContent.length());
        }
        return context;
    }
}