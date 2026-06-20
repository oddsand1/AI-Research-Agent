package com.ai.ai_research_agent.controller;


import com.ai.ai_research_agent.common.Result;
import com.ai.ai_research_agent.dto.RagSearchDTO;
import com.ai.ai_research_agent.dto.RagStoreDTO;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagRetrievalService ragRetrievalService;



    /**
     * 知识库文档上传（PDF/TXT）
     */
    @PostMapping("/upload")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file){
        try{
            String filename=file.getOriginalFilename();
            if(filename==null){
                return Result.fail(400,"文件名不能为空");
            }

            String content;
            if(filename.endsWith(".txt")){
                content=new String(file.getBytes(),"UTF-8");
            }else if(filename.endsWith(".pdf")){
                content=ragRetrievalService.extractPdfContent(file);
            }else{
                return Result.fail(400,"文件格式错误，仅支持TXT和PDF格式");
            }

            ragRetrievalService.storeDocument(content, "upload");
            return Result.success("文档上传并向量化入库成功");
        }catch(Exception e){
            return Result.fail(500,"文档读取失败："+e.getMessage());
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
}