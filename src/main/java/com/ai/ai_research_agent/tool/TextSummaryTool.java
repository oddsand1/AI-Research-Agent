package com.ai.ai_research_agent.tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


/**
 * 文本摘要工具（长文本精简）
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TextSummaryTool {

    private final ChatClient chatClient;

    @Tool(
            name = "text_summary",
            description="对大段文本进行摘要提炼，去除冗余内容，输出核心要点。入参为原始长文本。"
    )
    public String summary(@ToolParam(description = "需要摘要的原始长文本，不能为空") String content) {
        if(!StringUtils.hasText(content)){
            return "输入的文本不能为空";
        }
        log.info("【文本摘要工具】开始执行摘要，文本长度：{}", content.length());
        try{
            String result=chatClient.prompt()       //创建一个 Prompt 构建器, 相当于"打开一个新的对话窗口"
                    .user("请对以下内容进行精简摘要，保留核心信息：\n"+content)         //添加用户输入
                    .call()           //发送请求给 AI 模型并等待响应
                    .content();       //从响应中 提取纯文本内容
            log.info("【文本摘要工具】摘要完成");
            return StringUtils.hasText(result) ? result : "摘要结果为空";
        }catch (Exception e){
            log.error("【文本摘要工具】执行异常",e);
            return "文本摘要异常"+e.getMessage();
        }
    }
}
