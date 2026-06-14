package com.ai.ai_research_agent.tool;

// JSON结构化格式化工具


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JsonFormatTool {

    @Tool(
            name="json_format",
            description = "把自然语言文本转换成标准、格式化的JSON字符串，用于生成结构化报告。入参为原始文本内容。"
    )
    public String formatJson(@ToolParam(description = "待转换的文本内容") String content) {
        log.info("【JSON格式化工具】开始转换");
        try{
            if(!StringUtils.hasText(content)){
                return "{}";
            }
            String jsonStr= JSON.toJSONString(new ReportData(content), JSONWriter.Feature.PrettyFormat);
            log.info("【JSON格式化工具】转换完成");
            return "结构化JSON报告：\n" + jsonStr;
        } catch (Exception e) {
            log.error("【JSON格式化工具】执行异常", e);
            return "JSON格式化失败，原始内容：" + content;
        }
    }





    // 包装类，用于将原始文本内容转换为JSON格式，使其符合JSON规范
    private static class ReportData{

        //若要自定义字段名，可以使用 @JSONField(name = "content")
        private String reportContent;

        public ReportData(String reportContent) {
            this.reportContent = reportContent;
        }

        public String getReportContent() {//FastJSON 会扫描类中的所有 getter 方法(以get开头),并根据方法名生成JSON key(字段名)
            return reportContent;
        }

        public void setReportContent(String reportContent) {
            this.reportContent = reportContent;
        }
    }
}
