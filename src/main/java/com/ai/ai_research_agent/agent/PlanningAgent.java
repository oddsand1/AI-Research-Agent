package com.ai.ai_research_agent.agent;

import com.ai.ai_research_agent.context.AgentContext;
import com.ai.ai_research_agent.tool.PdfTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规划智能体：全局任务调度中心
 * 基于 ReAct 模式，每步由 LLM 根据当前状态动态决策下一步动作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanningAgent {

    private final ChatModel chatModel;
    private final SearchAgent searchAgent;
    private final RAGAgent ragAgent;
    private final AnalyzeAgent analyzeAgent;
    private final ReportAgent reportAgent;
    private final PdfTool pdfTool;

    private static final int MAX_STEPS = 5;
    private static final Set<String> ACTIONS = Set.of("SEARCH", "RAG", "PDF_SEARCH", "PDF_READ", "PDF_TABLE", "PDF_IMAGE", "ANALYZE", "REPORT", "STOP");

    public AgentContext planTask(String userQuery, Long taskId) {
        log.info("【规划智能体】开始动态调度，任务ID：{}，提问：{}", taskId, userQuery);
        AgentContext context = new AgentContext();
        context.setTaskId(taskId);
        context.setUserQuery(userQuery);
        context.setTaskStatus("RUN");

        List<String> history = new ArrayList<>();

        try {
            for (int step = 1; step <= MAX_STEPS; step++) {
                String action = decideNextAction(context, history);
                log.info("【规划智能体】第{}步决策：{}", step, action);
                history.add(action);

                switch (action) {
                    case "SEARCH" -> context = searchAgent.search(context);
                    case "RAG" -> context = ragAgent.retrieve(context);
                    case "PDF_SEARCH" -> {
                        String pdfResult = pdfTool.searchPdf(context.getUserQuery(), 3);
                        context.setRagContext(merge(context.getRagContext(), pdfResult));
                    }
                    case "PDF_READ" -> {
                        String docId = extractDocId(context);
                        int page = extractPage(context);
                        String pageContent = pdfTool.readPage(docId, page);
                        context.setRagContext(merge(context.getRagContext(), pageContent));
                    }
                    case "PDF_TABLE" -> {
                        String docId = extractDocId(context);
                        int page = extractPage(context);
                        String tableResult = pdfTool.extractTable(docId, page);
                        context.setRagContext(merge(context.getRagContext(), tableResult));
                    }
                    case "PDF_IMAGE" -> {
                        String docId = extractDocId(context);
                        int page = extractPage(context);
                        String imageResult = pdfTool.analyzeImage(docId, page);
                        context.setRagContext(merge(context.getRagContext(), imageResult));
                    }
                    case "ANALYZE" -> context = analyzeAgent.analyze(context);
                    case "REPORT" -> {
                        context = reportAgent.generateReport(context);
                        context.setTaskStatus("SUCCESS");
                        log.info("【规划智能体】报告生成完毕，任务{}完成", taskId);
                        return context;
                    }
                    case "STOP" -> {
                        context.setTaskStatus("SUCCESS");
                        log.info("【规划智能体】LLM决策终止，任务{}完成", taskId);
                        return context;
                    }
                }
            }

            // 达到最大步数，兜底生成报告
            log.warn("【规划智能体】达到最大步数{}，兜底生成报告", MAX_STEPS);
            if (context.getFinalReport() == null) {
                if (context.getAnalyzedContent() == null) {
                    context = analyzeAgent.analyze(context);
                }
                context = reportAgent.generateReport(context);
            }
            context.setTaskStatus("SUCCESS");

        } catch (Exception e) {
            context.setTaskStatus("FAIL");
            log.error("【规划智能体】任务{}执行异常", taskId, e);
        }
        return context;
    }

    /**
     * LLM 决策：根据当前上下文和历史，决定下一步动作
     */
    private String decideNextAction(AgentContext ctx, List<String> history) {
        String prompt = buildDecisionPrompt(ctx, history);
        String raw = chatModel.call(prompt);
        return normalize(ctx, raw);
    }

    /**
     * 构建决策 Prompt
     */
    private String buildDecisionPrompt(AgentContext ctx, List<String> history) {
        boolean hasSearch = notBlank(ctx.getRawSearchContent());
        boolean hasRag = notBlank(ctx.getRagContext());
        boolean hasAnalysis = notBlank(ctx.getAnalyzedContent());
        boolean hasReport = notBlank(ctx.getFinalReport());

        long searchCount = history.stream().filter("SEARCH"::equals).count();
        long ragCount = history.stream().filter("RAG"::equals).count();
        int maxPerAction = 2;

        return String.format("""
            你是任务调度专家，根据当前状态决策下一步动作。

            【用户问题】%s
            【已执行】%s
            【当前状态】
            - 搜索内容：%s
            - RAG知识库：%s
            - 分析结果：%s
            - 报告：%s

            可选动作：SEARCH / RAG / PDF_SEARCH / PDF_READ / PDF_TABLE / PDF_IMAGE / ANALYZE / REPORT / STOP

            决策规则：
            1. 涉及最新信息优先SEARCH，涉及历史知识优先RAG
            2. 问题明确提到PDF文档内容 → PDF_SEARCH 语义检索PDF片段
            3. 需要阅读特定页面 → PDF_READ doc_id=文档名 page=页码
            4. 需要提取表格数据 → PDF_TABLE doc_id=文档名 page=页码
            5. 需要查看图片/图表 → PDF_IMAGE doc_id=文档名 page=页码
            6. 每项最多执行%d次（SEARCH已%d次，RAG已%d次）
            7. 已有搜索和RAG内容但未分析 → ANALYZE
            8. 分析完成 → REPORT
            9. 报告已生成 → STOP
            10. 从未执行过任何动作 → 根据问题类型选SEARCH或RAG或PDF_SEARCH

            PDF_READ/PDF_TABLE/PDF_IMAGE 需附带 doc_id=xxx page=页码。只输出一个动作名称。
            """,
            ctx.getUserQuery(),
            history.isEmpty() ? "无" : String.join(" → ", history),
            hasSearch ? "有" : "无",
            hasRag ? "有" : "无",
            hasAnalysis ? "有" : "无",
            hasReport ? "有" : "无",
            maxPerAction, searchCount, ragCount
        );
    }

    /**
     * 标准化 LLM 输出为有效动作名，同时解析 docId 和 pageNum
     */
    private String normalize(AgentContext ctx, String raw) {
        String upper = raw.trim().toUpperCase();

        Matcher dm = Pattern.compile("DOC_ID[=:]\\s*(\\S+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (dm.find()) ctx.setDocId(dm.group(1).replaceAll("[\"']", ""));

        Matcher pm = Pattern.compile("PAGE[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (pm.find()) ctx.setPageNum(Integer.parseInt(pm.group(1)));

        for (String action : ACTIONS) {
            if (upper.contains(action)) {
                return action;
            }
        }
        log.warn("【规划智能体】LLM输出无法识别：{}，默认STOP", raw);
        return "STOP";
    }

    private String merge(String existing, String incoming) {
        if (existing == null || existing.isBlank()) return incoming;
        return existing + "\n\n" + incoming;
    }

    private String extractDocId(AgentContext ctx) {
        return ctx.getDocId() != null ? ctx.getDocId() : "unknown";
    }

    private int extractPage(AgentContext ctx) {
        return ctx.getPageNum() != null ? ctx.getPageNum() : 1;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}