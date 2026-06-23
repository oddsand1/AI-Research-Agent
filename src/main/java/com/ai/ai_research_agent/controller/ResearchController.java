package com.ai.ai_research_agent.controller;


import com.ai.ai_research_agent.context.AgentContext;
import com.ai.ai_research_agent.common.Result;
import com.ai.ai_research_agent.dto.ReportQueryDTO;
import com.ai.ai_research_agent.dto.ResearchRequestDTO;
import com.ai.ai_research_agent.service.ReportService;
import com.ai.ai_research_agent.service.ResearchService;
import com.ai.ai_research_agent.vo.ReportListVO;
import com.ai.ai_research_agent.vo.ReportResponseVO;
import com.ai.ai_research_agent.vo.ResearchResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;
    private final ReportService reportService;



    /**
     * 课题调研提交
     */
    @PostMapping("/submit")
    public Result<ResearchResponseVO> submit(@Valid @RequestBody ResearchRequestDTO dto){
        AgentContext context=researchService.executeResearch(dto.getUserQuery());

        ResearchResponseVO vo=new ResearchResponseVO();
        vo.setTaskId(context.getTaskId());
        vo.setStatus(context.getTaskStatus());
        vo.setReport(context.getFinalReport());
        vo.setMessage("调研完成");

        return Result.success(vo);
    }



    /**
     * 历史记录查询（按关键词/时间）
     */
    @GetMapping("/reports")
    public Result<ReportListVO> listReports(@Valid ReportQueryDTO dto){
        List<ReportResponseVO> reports =reportService.listReports(dto);
        ReportListVO vo=new ReportListVO();
        vo.setReports(reports);
        vo.setPageNum(dto.getPageNum());
        vo.setPageSize(dto.getPageSize());
        return Result.success(vo);
    }



    /**
     * 查看单条报告详情
     */
    @GetMapping("/report/{taskId}")
    public Result<ReportResponseVO> getReport(@PathVariable Long taskId){
        ReportResponseVO vo=reportService.getReportByTaskId(taskId);
        return Result.success(vo);
    }
}
