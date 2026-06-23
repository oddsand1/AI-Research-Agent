package com.ai.ai_research_agent.agent;

import com.ai.ai_research_agent.context.AgentContext;
import com.ai.ai_research_agent.tool.TextSummaryTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeAgent {

    private final ChatModel chatModel;
    private final TextSummaryTool textSummaryTool;

    private static final int MAX_INPUT_LENGTH = 6000;

    public AgentContext analyze(AgentContext context) {
        log.info("【分析智能体】开始分析，任务ID：{}", context.getTaskId());
        try {
            // 1. 合并多源信息
            String merged = buildMergedContent(context);
            if (merged.isEmpty()) {
                context.setAnalyzedContent("无可分析内容：搜索和RAG均为空");
                return context;
            }

            // 2. 长文本先压缩
            if (merged.length() > MAX_INPUT_LENGTH) {
                log.info("【分析智能体】内容过长({})，先摘要压缩", merged.length());
                merged = textSummaryTool.summary(merged);
            }

            // 3. 去重 + 冲突检测 + 结构化分析
            String result = deepAnalyze(merged, context.getUserQuery());
            context.setAnalyzedContent(result);
            log.info("【分析智能体】分析完成，结果长度：{}", result.length());

        } catch (Exception e) {
            log.error("【分析智能体】分析异常", e);
            context.setAnalyzedContent("分析失败：" + e.getMessage());
        }
        return context;
    }

    /**
     * 合并搜索和RAG内容，标注来源
     */
    private String buildMergedContent(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(ctx.getRawSearchContent())) {
            sb.append("【来源：联网搜索】\n").append(ctx.getRawSearchContent()).append("\n\n");
        }
        if (notBlank(ctx.getRagContext())) {
            sb.append("【来源：历史知识库】\n").append(ctx.getRagContext()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 深度分析：去重、冲突检测、结构化输出
     */
    private String deepAnalyze(String content, String userQuery) {
        String prompt = String.format("""
            你是信息分析专家，请对以下多源信息进行深度分析，围绕"%s"输出结构化结果：

            【分析要求】
            1. 去重：识别并合并重复信息，只保留一份
            2. 冲突检测：标注不同来源间的矛盾点，格式【冲突】...
            3. 分类整理：按主题归类，每个主题下列出关键发现
            4. 信息缺口：标注缺失的关键信息，格式【缺口】...
            5. 可信度：对每个关键发现标注可信度（高/中/低）
            6. 严格基于原文：只使用上面提供的多源信息，不得编造或推测
            7. 如果信息不足以回答用户问题，直接输出"信息不足，无法完成分析"，不要强行输出

            【输出格式】
            ## 核心发现
            ### 主题一
            - 发现1（可信度：高，来源：联网搜索）
            - 发现2（可信度：中，来源：历史知识库）
            【冲突】...
            ### 主题二
            ...

            ## 矛盾与冲突
            （如无冲突则写"未发现明显矛盾"）

            ## 信息缺口
            【缺口】...

            【待分析内容】
            %s
            """, userQuery, content);

        return chatModel.call(prompt).trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}