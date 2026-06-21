package com.ai.ai_research_agent.mapper;

import com.ai.ai_research_agent.entity.PdfChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PdfChunkMapper extends BaseMapper<PdfChunk> {

    @Select("SELECT * FROM pdf_chunk WHERE doc_id = #{docId} ORDER BY page_num")
    List<PdfChunk> selectByDocId(@Param("docId") String docId);

    @Select("SELECT * FROM pdf_chunk WHERE doc_id = #{docId} AND page_num = #{pageNum}")
    List<PdfChunk> selectByPage(@Param("docId") String docId, @Param("pageNum") int pageNum);

    @Select("SELECT * FROM pdf_chunk WHERE doc_id = #{docId} AND chunk_type = #{chunkType} ORDER BY page_num")
    List<PdfChunk> selectByType(@Param("docId") String docId, @Param("chunkType") String chunkType);
}