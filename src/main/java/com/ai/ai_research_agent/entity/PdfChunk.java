package com.ai.ai_research_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 分块实体：带完整文档元数据，用于语义切片和可追溯引用
 * 对应数据库表 pdf_chunk
 */
@Data
public class PdfChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的 VectorKnowledge ID */
    private Long vectorId;
    /** 分块文本内容 */
    private String content;
    /** 分块类型：text/table/image */
    private String chunkType;
    /** 页码 */
    private Integer pageNum;
    /** 总页数 */
    private Integer totalPages;
    /** 文档ID（文件名） */
    private String docId;
    /** 文档标题 */
    private String docTitle;
    /** 所属章节标题 */
    private String sectionTitle;
    /** 上下文说明（供引用时展示位置） */
    private String context;
    /** 表格结构化数据（chunkType=table 时，JSON 格式） */
    private String tableData;
    /** 图片多模态描述（chunkType=image 时） */
    private String imageCaption;
    /** 创建时间 */
    private LocalDateTime createTime;
}