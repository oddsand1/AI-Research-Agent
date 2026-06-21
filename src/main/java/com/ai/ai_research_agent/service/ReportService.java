package com.ai.ai_research_agent.service;


import com.ai.ai_research_agent.dto.ReportQueryDTO;
import com.ai.ai_research_agent.entity.ResearchReport;
import com.ai.ai_research_agent.mapper.ResearchReportMapper;
import com.ai.ai_research_agent.rag.RagRetrievalService;
import com.ai.ai_research_agent.vo.ReportResponseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ResearchReportMapper researchReportMapper;
    private final RagRetrievalService ragRetrievalService;

    public void saveReport(Long taskId, String content) {
        ResearchReport report = new ResearchReport();
        report.setTaskId(taskId);
        report.setContent(content);
        report.setCreateTime(LocalDateTime.now());
        researchReportMapper.insert(report);
        log.info("报告已入库，报告ID：{}", report.getId());
    }

    public void vectorizeReport(String content,String source) {
        ragRetrievalService.storeDocument(content,source);
        log.info("报告已向量化入库");
    }


    /**
     * 按条件分页查询报告列表
     */
    public List<ReportResponseVO> listReports(ReportQueryDTO dto) {
        LambdaQueryWrapper<ResearchReport> wrapper = new LambdaQueryWrapper<>();
        if (dto.getTaskId() != null) {
            wrapper.eq(ResearchReport::getTaskId, dto.getTaskId());
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            wrapper.like(ResearchReport::getContent, dto.getKeyword());
        }
        if (dto.getStartTime() != null && !dto.getStartTime().isBlank()) {
            wrapper.ge(ResearchReport::getCreateTime, LocalDateTime.parse(dto.getStartTime()));
        }
        if (dto.getEndTime() != null && !dto.getEndTime().isBlank()) {
            wrapper.le(ResearchReport::getCreateTime, LocalDateTime.parse(dto.getEndTime()));
        }
        wrapper.orderByDesc(ResearchReport::getCreateTime);

        //创建MP分页对象
        Page<ResearchReport> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<ResearchReport> result = researchReportMapper.selectPage(page, wrapper);
        return result.getRecords()
                // 获取分页查询出来的当前页数据库列表 List<ResearchReport>
                .stream()
                // 开启流式处理
                .map(r -> {
                    // r：循环每一条数据库实体ResearchReport
                    ReportResponseVO vo = new ReportResponseVO();
                    vo.setId(r.getId());
                    vo.setTaskId(r.getTaskId());
                    vo.setContent(r.getContent());
                    vo.setCreateTime(r.getCreateTime());
                    return vo;
                })
                // 将转换后的所有VO收集成List<ReportResponseVO>返回
                .collect(Collectors.toList());
    }


    /**
     * 按任务ID查询报告
     */
    public ReportResponseVO getReportByTaskId(Long taskId) {
        LambdaQueryWrapper<ResearchReport> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(ResearchReport::getTaskId, taskId);
        wrapper.orderByDesc(ResearchReport::getCreateTime);
        wrapper.last("limit 1");
        ResearchReport report = researchReportMapper.selectOne(wrapper);
        if(report==null){
            throw new IllegalArgumentException("未找到该任务的报告");
        }
        ReportResponseVO vo = new ReportResponseVO();
        vo.setId(report.getId());
        vo.setTaskId(report.getTaskId());
        vo.setContent(report.getContent());
        vo.setCreateTime(report.getCreateTime());
        return vo;
    }

}