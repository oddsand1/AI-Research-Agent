package com.ai.ai_research_agent.mapper;

import com.ai.ai_research_agent.entity.VectorKnowledge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VectorKnowledgeMapper extends BaseMapper<VectorKnowledge> {

    @Select("SELECT *, 1 - (embedding <=> #{embedding}::vector) AS similarity " +
            "FROM vector_knowledge " +
            "WHERE 1 - (embedding <=> #{embedding}::vector) > #{threshold} " +
            "ORDER BY embedding <=> #{embedding}::vector LIMIT #{topK}")
    List<VectorKnowledge> vectorSearch(@Param("embedding") String embedding,
                                       @Param("threshold") double threshold,
                                       @Param("topK") int topK);
}