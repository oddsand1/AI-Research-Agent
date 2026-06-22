package com.ai.ai_research_agent.controller;


import com.ai.ai_research_agent.common.Result;
import com.ai.ai_research_agent.dto.RagSearchDTO;
import com.ai.ai_research_agent.dto.RagStoreDTO;
import com.ai.ai_research_agent.rag.EvalService;
import com.ai.ai_research_agent.rag.PdfParserService;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagRetrievalService ragRetrievalService;
    private final PdfParserService pdfParserService;
    private final EvalService evalService;



    /**
     * 知识库文档上传（PDF/TXT）
     */
    @PostMapping("/upload")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return Result.fail(400, "文件名不能为空");
            }

            if (filename.endsWith(".txt")) {
                String content = new String(file.getBytes(), "UTF-8");
                ragRetrievalService.storeDocument(content, "upload");
                return Result.success("TXT文档上传并向量化入库成功");
            } else if (filename.endsWith(".pdf")) {
                int count = pdfParserService.parseAndStore(file).size();
                return Result.success("PDF解析完成，共" + count + "个分块，已双落库");
            } else {
                return Result.fail(400, "文件格式错误，仅支持TXT和PDF格式");
            }
        } catch (Exception e) {
            return Result.fail(500, "文档处理失败：" + e.getMessage());
        }
    }


    /**
     * 文本内容直接入库
     */
    @PostMapping("/store")
    public Result<String> storeDocument(@Valid @RequestBody RagStoreDTO dto){
        ragRetrievalService.storeDocument(dto.getContent(), "upload");
        return Result.success("文档入库+向量化完成");
    }



    /**
     * 相似问题检索（Top5）
     */
    @GetMapping("/search")
    public Result<String> searchSimilar(@Valid RagSearchDTO dto) {
        String context = ragRetrievalService.retrieveContext(dto.getQuery(), dto.getTopK());
        if (context.isEmpty()) {
            return Result.fail(404, "未找到相关文档");
        }
        return Result.success(context);
    }

    /**
     * 检索评测接口
     */
    @PostMapping("/eval")
    public Result<String> eval(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) body.get("keywords");
        String answer = (String) body.get("answer");
        String groundTruth = (String) body.get("groundTruth");

        if (query == null) {
            return Result.fail(400, "query不能为空");
        }

        EvalService.FullEvalResult result = evalService.fullEval(
                query, keywords != null ? keywords : List.of(),
                answer != null ? answer : "",
                groundTruth != null ? groundTruth : ""
        );
        return Result.success(result.summary());
    }
}