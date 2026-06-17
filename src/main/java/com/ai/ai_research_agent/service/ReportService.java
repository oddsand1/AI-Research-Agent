package com.ai.ai_research_agent.service;


import com.ai.ai_research_agent.entity.ResearchReport;
import com.ai.ai_research_agent.mapper.ResearchReportMapper;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ResearchReportMapper researchReportMapper;
    private final RagRetrievalService ragRetrievalService;

    public void saveReport(Long taskId,String content) {
        ResearchReport report =new ResearchReport();
        report.setTaskId(taskId);
        report.setContent(content);
        report.setCreateTime(LocalDateTime.now());
        researchReportMapper.insert(report);
        log.info("报告已入库，报告ID：{}", report.getId());
    }

    public void vectorizeReport(String content) {
        ragRetrievalService.storeDocument(content);
        log.info("报告已向量化入库");
    }
}
