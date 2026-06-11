package com.ai.ai_research_agent.controller;

import com.ai.ai_research_agent.common.Result;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RAG 功能测试接口
 */
@Slf4j
@RestController
@RequestMapping("/rag")
public class RagTestController {

    private final RagRetrievalService ragRetrievalService;

    public RagTestController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    /**
     * 文档向量化入库
     */
    @GetMapping("/store")
    public Result<String> store(@RequestParam String content) {
        ragRetrievalService.storeDocument(content);
        return Result.success("文档入库+向量化完成");
    }

    @GetMapping("/s")
    public Result<String> s1(@RequestParam String content) {
        return Result.success("完成");
    }
    /**
     * 向量检索
     */
    @GetMapping("/search")
    public Result<String> search(@RequestParam String query) {
        String context = ragRetrievalService.retrieveContext(query, 3);
        return Result.success(context);
    }

    /**
     * 清空知识库
     */
    @GetMapping("/clear")
    public Result<String> clear() {
        ragRetrievalService.clearAllKnowledge();
        return Result.success("知识库已清空");
    }


}