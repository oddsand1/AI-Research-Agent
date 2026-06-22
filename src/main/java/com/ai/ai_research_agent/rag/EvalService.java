package com.ai.ai_research_agent.rag;


import com.ai.ai_research_agent.entity.VectorKnowledge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 评测服务：检索命中率、引用准确性、回答质量评估
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    private final RagRetrievalService ragRetrievalService;
    private final ChatModel chatModel;

    /**
     * 检索命中率评测：检查检索到的片段是否包含预期关键词
     */
    public EvalResult evalRetrieval(String query, List<String> expectedKeywords) {
        List<VectorKnowledge> retrieved = ragRetrievalService.retrieveWithMeta(query, 5);
        int hitCount = 0;
        List<String> hitDetails = new ArrayList<>();

        for (String keyword : expectedKeywords) {
            boolean found = retrieved.stream()
                    .anyMatch(vk -> vk.getContent().contains(keyword));
            if (found) {
                hitCount++;
                hitDetails.add("✓ " + keyword);
            } else {
                hitDetails.add("✗ " + keyword);
            }
        }

        double hitRate = expectedKeywords.isEmpty() ? 0 :
                (double) hitCount / expectedKeywords.size();

        return new EvalResult(
                query, retrieved.size(), hitCount, expectedKeywords.size(),
                hitRate, hitDetails
        );
    }

    /**
     * 引用准确性评测：检查回答中是否包含正确的页码引用
     */
    public CitationEval evalCitation(String answer) {
        Pattern pattern = Pattern.compile("第(\\d+)页");
        Matcher matcher = pattern.matcher(answer);
        List<Integer> citedPages = new ArrayList<>();
        while (matcher.find()) {
            citedPages.add(Integer.parseInt(matcher.group(1)));
        }

        boolean hasCitation = !citedPages.isEmpty();
        boolean hasDocTitle = answer.contains("来源：《") || answer.contains("《");

        return new CitationEval(hasCitation, hasDocTitle, citedPages);
    }

    /**
     * 回答质量评测：LLM 打分
     */
    public QualityEval evalAnswerQuality(String query, String answer, String groundTruth) {
        String prompt = String.format("""
            你是RAG质量评测专家，请对以下回答进行打分（1-5分）。

            【用户问题】%s
            【模型回答】%s
            【标准答案】%s

            评测维度：
            1. 准确性（回答是否与标准答案一致）
            2. 完整性（是否覆盖所有关键点）
            3. 引用质量（是否标注了来源和页码）
            4. 简洁性（是否无冗余信息）

            请输出JSON格式：
            {"accuracy": 4, "completeness": 3, "citation": 5, "conciseness": 4, "comment": "评价说明"}
            """, query, answer, groundTruth);

        try {
            String raw = chatModel.call(prompt).trim();
            return parseQuality(raw);
        } catch (Exception e) {
            log.warn("质量评测失败：{}", e.getMessage());
            return new QualityEval(0, 0, 0, 0, "评测异常");
        }
    }

    /**
     * 综合评测入口
     */
    public FullEvalResult fullEval(String query, List<String> expectedKeywords,
                                   String answer, String groundTruth) {
        EvalResult retrieval = evalRetrieval(query, expectedKeywords);
        CitationEval citation = evalCitation(answer);
        QualityEval quality = evalAnswerQuality(query, answer, groundTruth);

        return new FullEvalResult(retrieval, citation, quality);
    }

    private QualityEval parseQuality(String raw) {
        try {
            String json = raw.replaceAll("```json|```", "").trim();
            int acc = extractInt(json, "accuracy");
            int comp = extractInt(json, "completeness");
            int cit = extractInt(json, "citation");
            int conc = extractInt(json, "conciseness");
            String comment = extractString(json, "comment");
            return new QualityEval(acc, comp, cit, conc, comment);
        } catch (Exception e) {
            return new QualityEval(0, 0, 0, 0, raw);
        }
    }

    private int extractInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // ========= 评测结果类 =========

    public record EvalResult(String query, int retrievedCount, int hitCount,
                             int totalKeywords, double hitRate, List<String> details) {
        public String summary() {
            return String.format("检索命中率：%d/%d (%.0f%%)，召回%d条",
                    hitCount, totalKeywords, hitRate * 100, retrievedCount);
        }
    }

    public record CitationEval(boolean hasCitation, boolean hasDocTitle, List<Integer> citedPages) {
        public String summary() {
            return String.format("引用：%s，文档标题：%s，引用页码：%s",
                    hasCitation ? "有" : "无",
                    hasDocTitle ? "有" : "无",
                    citedPages.isEmpty() ? "无" : citedPages.toString());
        }
    }

    public record QualityEval(int accuracy, int completeness, int citation, int conciseness,
                              String comment) {
        public double avgScore() {
            return (accuracy + completeness + citation + conciseness) / 4.0;
        }

        public String summary() {
            return String.format("综合%.1f分 | 准确%d 完整%d 引用%d 简洁%d | %s",
                    avgScore(), accuracy, completeness, citation, conciseness, comment);
        }
    }

    public record FullEvalResult(EvalResult retrieval, CitationEval citation, QualityEval quality) {
        public String summary() {
            return String.format("""
                    ===== RAG 评测报告 =====
                    %s
                    %s
                    %s
                    """,
                    retrieval.summary(), citation.summary(), quality.summary());
        }
    }
}
