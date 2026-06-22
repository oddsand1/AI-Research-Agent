package com.ai.ai_research_agent.rag;


import com.ai.ai_research_agent.entity.PdfChunk;
import com.ai.ai_research_agent.mapper.PdfChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * PDF 增强解析服务
 * 功能：类型检测 → 文本提取 → 表格识别 → 按页构建 PdfChunk → 双落库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfParserService {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    private final ChatModel chatModel;
    private final TextSplitter textSplitter;
    private final PdfChunkMapper pdfChunkMapper;
    private final RagRetrievalService ragRetrievalService;

    private static final int SCAN_THRESHOLD = 50; // 每页少于50字视为扫描件

    /**
     * 主入口：解析 PDF → 构建 PdfChunk → 双落库
     */
    public List<PdfChunk> parseAndStore(MultipartFile file) {
        String docId=file.getOriginalFilename();
        try(PDDocument doc= Loader.loadPDF(file.getBytes())){
            String docTitle=extractDocTitle(doc,docId);
            int totalPages=doc.getNumberOfPages();
            PdfType type =detectType(doc);

            log.info("PDF类型检测：{}，页数：{}，类型：{}", docId, totalPages, type);

            List<PdfChunk> allChunks=new ArrayList<>();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                List<PdfChunk> pageChunks=parsePage(doc, pageNum, docId, docTitle, totalPages, type);
                allChunks.addAll(pageChunks);
            }

            // 双落库：PdfChunk表 + VectorKnowledge表
            for(PdfChunk chunk:allChunks){
                chunk.setCreateTime(LocalDateTime.now());
                pdfChunkMapper.insert(chunk);
                // 向量化入库，带完整元数据
                ragRetrievalService.storeDocumentWithMeta(
                        chunk.getContent(), "upload",
                        chunk.getDocId(), chunk.getDocTitle(),
                        chunk.getPageNum(), chunk.getChunkType(),
                        chunk.getContext()
                );
            }

            log.info("PDF解析完成：{}，共{}个分块", docId, allChunks.size());
            return allChunks;
        }catch (IOException e){
            throw new IllegalArgumentException("PDF解析失败：" + e.getMessage(), e);
        }
    }




    /**
     * 检测 PDF 类型
     */
    private PdfType detectType(PDDocument doc) throws IOException {
        int totalPages=doc.getNumberOfPages();
        int scannedPages=0;
        PDFTextStripper stripper=new PDFTextStripper();

        for (int i = 1; i <= totalPages; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String text = stripper.getText(doc).trim();
            if (text.length() < SCAN_THRESHOLD) {
                scannedPages++;
            }
        }

        if (scannedPages == 0) return PdfType.NATIVE;
        if (scannedPages == totalPages) return PdfType.SCANNED;
        return PdfType.MIXED;
    }




    /**
     * 解析单页，返回该页的 PdfChunk 列表
     */
    private List<PdfChunk> parsePage(PDDocument doc,int pageNum,
                                     String docId,String docTitle,
                                     int totalPages,PdfType type) throws IOException {
        List<PdfChunk> chunks=new ArrayList<>();

        // 提取页面文本
        PDFTextStripper stripper=new PDFTextStripper();
        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        stripper.setSortByPosition(true);
        stripper.setAddMoreFormatting(true);
        String pageText=stripper.getText(doc).trim();

        if(type==PdfType.SCANNED || pageText.length()<SCAN_THRESHOLD) {
            // 扫描件：用 LLM 多模态 OCR
            String ocrText=ocrWithLLM(doc,pageNum,docId);
            if(ocrText!=null && !ocrText.isBlank()){
                pageText=ocrText;
            }
        }

        if(pageText.isBlank()) return chunks;

        // 检测表格区域
        List<String> tableBlocks=detectTableBlocks(pageText);
        String remainingText=pageText;
        for (String table : tableBlocks) {
            remainingText=remainingText.replace(table,"");
            PdfChunk tableChunk=buildChunk(table, "table", pageNum, totalPages, docId, docTitle, null);
            tableChunk.setTableData(structuredTable(table));
            chunks.add(tableChunk);
        }

        // 剩余文本按语义切分
        List<TextSplitter.ChunkMeta> textChunks=textSplitter.splitSemantic(remainingText);
        for (TextSplitter.ChunkMeta meta : textChunks) {
            String context = String.format("文档《%s》第%d/%d页【%s】",
                    docTitle, pageNum, totalPages,
                    meta.sectionTitle() != null ? meta.sectionTitle() : "正文");
            PdfChunk chunk = buildChunk(meta.content(), "text", pageNum, totalPages, docId, docTitle, context);
            chunk.setSectionTitle(meta.sectionTitle());
            chunks.add(chunk);
        }

        // 提取页面图片
        List<PdfChunk> imageChunks = extractImages(doc, pageNum, docId, docTitle, totalPages);
        chunks.addAll(imageChunks);

        return chunks;
    }




    /**
     * 提取页面中的图片，多模态 LLM 描述后构建 image 类型 chunk
     */
    private List<PdfChunk> extractImages(PDDocument doc, int pageNum,
                                         String docId, String docTitle, int totalPages) throws IOException {
        List<PdfChunk> imageChunks = new ArrayList<>();
        PDPage page = doc.getPage(pageNum - 1);
        PDResources resources = page.getResources();

        int imgIndex = 0;
        for (COSName name : resources.getXObjectNames()) {
            if (!(resources.getXObject(name) instanceof PDImageXObject image)) {
                continue;
            }
            imgIndex++;

            // 转 base64
            BufferedImage buffered = image.getImage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", bos);
            String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());

            // 多模态描述
            String caption = describeImage(base64, docTitle, pageNum, imgIndex);
            if (caption == null || caption.isBlank()) continue;

            String context = String.format("文档《%s》第%d/%d页，图片%d",
                    docTitle, pageNum, totalPages, imgIndex);
            PdfChunk chunk = buildChunk(caption, "image", pageNum, totalPages, docId, docTitle, context);
            chunk.setImageCaption(caption);
            imageChunks.add(chunk);

            log.info("图片描述完成：{} 第{}页 图片{}，描述长度：{}", docId, pageNum, imgIndex, caption.length());
        }
        return imageChunks;
    }




    /**
     * 多模态 LLM 描述图片
     */
    private String describeImage(String base64Image, String docTitle, int pageNum, int imgIndex) {
        try {
            String prompt = String.format(
                    "请描述这张图片的内容。如果图片是图表，请说明图表类型、数据趋势和关键信息；" +
                    "如果图片是示意图，请描述其结构和要点。文档《%s》第%d页。", docTitle, pageNum);
            return callDashScopeVision(prompt, base64Image);
        } catch (Exception e) {
            log.warn("图片描述失败：{} 第{}页 图片{}，{}", docTitle, pageNum, imgIndex, e.getMessage());
            return null;
        }
    }

    /**
     * 直接调 DashScope 多模态 API，不依赖 Spring AI Media
     */
    private String callDashScopeVision(String prompt, String base64Image) throws Exception {
        var url = new java.net.URL("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation");
        var conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + dashscopeApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String body = String.format("""
                {"model":"qwen-vl-max","input":{"messages":[{"role":"user","content":[
                {"text":"%s"},
                {"image":"data:image/png;base64,%s"}
                ]}]}}""", prompt.replace("\"", "\\\""), base64Image);

        try (var os = conn.getOutputStream()) {
            os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        String resp = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        conn.disconnect();

        var json = com.alibaba.fastjson2.JSON.parseObject(resp);
        return json.getJSONObject("output").getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content");
    }




    /**
     * LLM 多模态 OCR（扫描件用）
     */
    private String ocrWithLLM(PDDocument doc,int pageNum,String docId) {
        try {
            String prompt = String.format(
                    "请识别附件PDF第%d页的文字内容，逐行输出，保留标题层级和段落结构。", pageNum);
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.warn("OCR失败：{} 第{}页，{}", docId, pageNum, e.getMessage());
            return null;
        }
    }




    /**
     * 检测表格块（基于行内空格密度和数字占比）
     */
    private List<String> detectTableBlocks(String text){
        List<String> tables=new ArrayList<>();
        String[] lines=text.split("\n");
        StringBuilder buf=new StringBuilder();
        boolean inTable=false;

        for (String line : lines) {
            boolean isTableLine=isTableLike(line);
            if(isTableLine){
                if(!inTable){
                    buf.setLength(0);
                    inTable=true;
                }
                buf.append(line).append("\n");
            }else{
                if(inTable && buf.length()>0){
                    tables.add(buf.toString().trim());
                    buf.setLength(0);
                }
                inTable=false;
            }
        }

        if(inTable && buf.length()>0){
            tables.add(buf.toString().trim());
        }
        return tables;
    }

    private boolean isTableLike(String line) {
        if (line.length() < 10) return false;
        int spaces = 0;
        int digits = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') spaces++;
            if (Character.isDigit(c)) digits++;
        }
        return spaces >= 3 && (double) digits / line.length() > 0.05;
    }




    /**
     * 表格结构化：用 LLM 转 Markdown 表格
     */
    private String structuredTable(String tableText) {
        try {
            String prompt = String.format("""
                    将以下文本转为Markdown表格，保留行列关系。
                    如无法识别表格结构，原样返回。

                    %s
                    """, tableText);
            return chatModel.call(prompt).trim();
        } catch (Exception e) {
            return tableText;
        }
    }




    /**
     * 提取文档标题
     */
    private String extractDocTitle(PDDocument doc, String fileName) {
        try {
            String infoTitle = doc.getDocumentInformation().getTitle();
            if (infoTitle != null && !infoTitle.isBlank()) {
                return infoTitle;
            }
        } catch (Exception ignored) {}
        return fileName.replaceAll("\\.(pdf|txt)$", "");
    }

    private PdfChunk buildChunk(String content, String chunkType, int pageNum, int totalPages,
                                String docId, String docTitle, String context) {
        PdfChunk chunk = new PdfChunk();
        chunk.setContent(content);
        chunk.setChunkType(chunkType);
        chunk.setPageNum(pageNum);
        chunk.setTotalPages(totalPages);
        chunk.setDocId(docId);
        chunk.setDocTitle(docTitle);
        chunk.setContext(context);
        return chunk;
    }

    enum PdfType { NATIVE, SCANNED, MIXED }
}