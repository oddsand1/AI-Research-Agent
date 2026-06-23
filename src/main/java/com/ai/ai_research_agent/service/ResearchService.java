package com.ai.ai_research_agent.service;


import com.ai.ai_research_agent.context.AgentContext;
import com.ai.ai_research_agent.agent.PlanningAgent;
import com.ai.ai_research_agent.entity.ResearchTask;
import com.ai.ai_research_agent.mapper.ResearchTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
    * 研究任务服务 - 全局编排入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchService {

    private final PlanningAgent planningAgent;
    private final ResearchTaskMapper researchTaskMapper;


    /**
     * 发起研究任务（同步执行）
     */
    @Transactional
    public AgentContext executeResearch(String userQuery) {
        //1. 创建任务记录
        ResearchTask task=new ResearchTask();
        task.setUserQuery(userQuery);
        task.setStatus("RUN");
        task.setCreateTime(LocalDateTime.now());
        researchTaskMapper.insert(task);
        log.info("研究任务已创建，任务ID：{}", task.getId());

        //2. 委托 PlanningAgent 调度全链路
        AgentContext context=planningAgent.planTask(userQuery,task.getId());

        //3. 更新任务状态
        task.setStatus(context.getTaskStatus());
        researchTaskMapper.updateById(task);

        return context;
    }
}
