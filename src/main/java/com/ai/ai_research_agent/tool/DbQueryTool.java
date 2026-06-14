package com.ai.ai_research_agent.tool;


//数据库查询工具（查任务/报告/知识库）

import com.ai.ai_research_agent.entity.ResearchReport;
import com.ai.ai_research_agent.entity.ResearchTask;
import com.ai.ai_research_agent.entity.VectorKnowledge;
import com.ai.ai_research_agent.mapper.ResearchReportMapper;
import com.ai.ai_research_agent.mapper.ResearchTaskMapper;
import com.ai.ai_research_agent.mapper.VectorKnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DbQueryTool {

    private final ResearchTaskMapper taskMapper;
    private final VectorKnowledgeMapper knowledgeMapper;
    private final ResearchReportMapper reportMapper;

    @Tool(
            name="db_query",
            description="查询系统历史数据。type支持三种类型：task（查询研究任务）、report（查询研究报告）、knowledge（查询知识库内容）、keyword为模糊查询关键词"
    )
    public String dbQuery(
            @ToolParam(description = "查询类型：task/report/knowledge") String type,
            @ToolParam(description = "模糊查询关键词，可为空") String keyword
    ){
        log.info("【数据库查询工具】type:{}, keyword:{}", type, keyword);
        try{
            if(!StringUtils.hasText(type)){
                return "查询类型不能为空";
            }
            return switch(type.trim()){
                case "task"->queryTask(keyword);
                case "report"->queryReport(keyword);
                case "knowledge"->queryKnowledge(keyword);
                default->"查询类型错误,仅支持task/report/knowledge类型";
            };
        } catch (Exception e) {
            log.error("【数据库查询工具】执行异常", e);
            return "数据库查询异常：" + e.getMessage();
        }
    }



    private String queryTask(String keyword){
        LambdaQueryWrapper<ResearchTask> wrapper=new LambdaQueryWrapper<>();
        if(StringUtils.hasText(keyword)){
            wrapper.like(ResearchTask::getUserQuery, keyword);
        }
        List<ResearchTask> list=taskMapper.selectList(wrapper);
        return "历史任务数据:"+list;
    }



    private String queryReport(String keyword){
        LambdaQueryWrapper<ResearchReport> wrapper=new LambdaQueryWrapper<>();
        if(StringUtils.hasText(keyword)){
            wrapper.like(ResearchReport::getContent,keyword);
        }
        List<ResearchReport> list=reportMapper.selectList(wrapper);
        return "历史报告数据:"+list;//返回整个对象，包含实体类里的所有信息
    }



    private String queryKnowledge(String keyword){
        LambdaQueryWrapper<VectorKnowledge> wrapper=new LambdaQueryWrapper<>();
        if(StringUtils.hasText(keyword)){
            wrapper.like(VectorKnowledge::getContent,keyword);
        }
        List<VectorKnowledge> list=knowledgeMapper.selectList(wrapper);
        return "历史知识库数据:"+list.stream().map(VectorKnowledge::getContent).toList();//提取内容，去除冗余信息
    }
}
