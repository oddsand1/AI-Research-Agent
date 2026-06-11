package com.ai.ai_research_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResearchTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userQuery;
    private String status;
    private LocalDateTime createTime;
}
