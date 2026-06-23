# AI Research Agent \- 多智能体深度调研系统

**✨ 基于 Spring AI \+ ReAct 多Agent 协作的企业级深度调研系统**

一站式智能调研解决方案，整合联网全网搜索、私有化RAG知识库、PDF全模态解析能力，全自动生成**带溯源引用、可核验、低幻觉**标准化Markdown调研报告，适配行业研究、学术调研、竞品分析、文档研判等业务场景。

⭐ 开源不易，欢迎 Star \| Fork \| Issue，持续迭代优化

---

## 📌 项目核心简介

区别于固定流程AI项目，本项目基于**ReAct自主决策模式**拆分5大职责Agent，由调度Agent动态决策执行步骤，无需硬编码业务流程；自研五层优化RAG检索链路，兼容原生/扫描/混合三类PDF文件全要素解析，搭建三层AI幻觉拦截机制，所有结论绑定文档页码、网页来源溯源，彻底解决大模型调研编造信息、论据无依据、文件识别不全痛点。

## 🛠️ 全套技术栈

|架构层级|选用技术|用途说明|
|---|---|---|
|后端框架|Spring Boot 3\.2\.5 \+ Java 21|高性能业务底座，适配虚拟线程高并发|
|AI 生态|Spring AI 1\.1\.2 \+ 阿里云DashScope|统一AI调用标准，通义qwen\-vl\-max大模型|
|向量数据库|PostgreSQL 15 \+ pgvector|私有化向量存储、语义相似度检索|
|持久层框架|MyBatis\-Plus 3\.5\.10|业务数据表便捷CRUD|
|PDF解析引擎|Apache PDFBox 3\.0|原生PDF结构化文本提取|
|多模态能力|DashScope多模态API|扫描件OCR、图片内容识别、图表解析|
|序列化工具|fastjson2|高性能JSON序列化、AI参数封装|

---

## 🤖 五大Agent分工架构

全链路 ReAct 自主循环调度，最大执行6步闭环，LLM自主判断：搜索/查库/读PDF/分析/出报告，无固定硬编码流程

1. **PlanningAgent 调度大脑**：核心决策中枢，ReAct循环管控，研判调研缺口、选定下一步执行工具、管控全流程步数

2. **SearchAgent 全网搜索Agent**：调研关键词智能提炼、合规全网检索、网页智能抓取、正文降噪摘要

3. **RAGAgent 知识库检索Agent**：私有化知识库混合检索、结果重排序、合规内容召回、溯源信息封装

4. **AnalyzeAgent 研判分析Agent**：多源信息去重、观点冲突校验、信息缺口标注、无效数据过滤

5. **ReportAgent 报告生成Agent**：标准化Markdown章节撰写、来源引用绑定、报告结构化入库、章节向量化归档

### 调研全链路流转

用户调研提问 → PlanningAgent动态调度 → 可选分支：全网搜索 / 私有知识库检索 / PDF专项解析 → AnalyzeAgent合规核验 → ReportAgent生成带溯源调研报告

---

## 🔥 核心差异化能力

### 1\. 五层精细化RAG检索体系（大幅提升检索准确率）

用户提问 → ① LLM问句扩写（自动生成4条语义变体） → ② 向量语义\+关键词双路并行检索 → ③ RRF倒数排名融合加权去重 → ④ LLM智能Rerank逐条语义打分精排 → ⑤ 标准化溯源输出 `[来源：《文档名》第n页]`

### 2\. 全模态PDF增强解析（市面少有的全覆盖能力）

|细分能力|技术实现方案|
|---|---|
|PDF智能分类|逐页文本密度研判，自适应区分：原生PDF/扫描PDF/混合版式PDF|
|高精度文本提取|PDFBox位置排序排版还原，保留原文段落、换行、格式语义|
|扫描件OCR识别|DashScope多模态逐页视觉OCR，适配模糊、倾斜电子化扫描文件|
|表格智能解析|版面空格密度算法识别表格区块，LLM一键转标准Markdown表格|
|内嵌图片解读|图片Base64编码转换，调用多模态模型解读图表、流程图、实景图|
|智能语义切片|依托标题层级语义切分，保留章节上下文，避免切片语义断裂|
|双库同步落库|结构化PDF分片库 \+ 向量语义知识库双向存储，适配不同检索场景|

### 3\. 三层闭环防大模型幻觉

- 第一层：RAGAgent检索兜底：知识库/全网无有效信息，直接日志告警终止链路

- 第二层：AnalyzeAgent研判兜底：素材残缺、观点冲突，标注信息不足，拒绝主观推演

- 第三层：ReportAgent生成兜底：核验分析内容有效性，空素材直接驳回，提示补充检索

### 4\. 全流程可溯源\+量化评测

- 所有调研观点、数据、结论绑定网页链接/文档名称/页码，一键溯源核验

- 内置EvalService评测模块：支持检索命中率、引用合规性、报告质量三维量化打分

---

## 🔌 后端REST API接口

### 调研任务接口

|请求方式|接口地址|业务说明|
|---|---|---|
|POST|`/research/submit`|提交调研需求，异步/同步返回溯源调研报告|
|GET|`/research/reports`|分页查询历史全部调研报告|

### 私有化知识库管理接口

|请求方式|接口地址|业务说明|
|---|---|---|
|POST|`/rag/upload`|上传PDF/TXT文件，自动解析、切片、向量化入库|
|POST|`/rag/store`|自定义文本内容，直接向量化入库搭建知识库|
|GET|`/rag/search`|主动检索知识库TopK高相似度片段|
|POST|`/rag/eval`|执行RAG检索效果量化评测，输出评分报告|

---

## 🚀 本地快速部署启动

### 前置环境要求

- JDK 21 及以上版本（必须，适配SpringBoot虚拟线程特性）

- Maven 3\.8\+

- PostgreSQL 15 \+ 预装 pgvector 向量扩展插件

- 阿里云DashScope有效API\-Key

### 步骤1：数据库初始化

```sql
-- 开启向量扩展（必须执行）
CREATE EXTENSION IF NOT EXISTS vector;

-- 项目支持两种建表方式
-- 方式1：开启配置自动建表：spring.sql.init.mode=always
-- 方式2：手动执行entity对应数据表SQL，适配自有数据库环境
```

### 步骤2：项目配置密钥

编辑路径：`src/main/resources/application.yml`

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的阿里云通义API_KEY
      chat:
        options:
          model: qwen-vl-max # 多模态对话大模型
      embedding:
        options:
          model: text-embedding-v3 # 官方高精度向量模型
```

### 步骤3：项目编译启动

```bash
# 依赖清理编译
mvn clean compile
# 启动SpringBoot项目
mvn spring-boot:run
```

服务默认地址：[http://localhost:8080](http://localhost:8080)

### 步骤4：curl快速接口调试

```bash
# 1、上传PDF构建私有知识库
curl -F "file=@本地技术白皮书.pdf" http://localhost:8080/rag/upload

# 2、提交专业调研任务
curl -X POST http://localhost:8080/research/submit \
  -H "Content-Type: application/json" \
  -d '{"userQuery": "Transformer 注意力机制核心原理与演进方向"}'

# 3、检索私有知识库内容
curl "http://localhost:8080/rag/search?query=注意力机制原理&topK=5"
```

---

## 💡 架构核心亮点总结

- **智能调度**：原生ReAct调度，LLM自主决策流程，摒弃传统固定if\-else业务流程

- **检索高精**：双路检索\+RRF融合\+LLM重排四层提纯，适配专业深度调研场景

- **文件兼容**：原生/扫描/混合PDF全适配，图文表格一站式提取解析

- **可信输出**：全文本溯源引用，每一条结论均可核验，合规性更强

- **低幻觉保障**：检索\-分析\-生成三层拦截，杜绝AI编造数据、编造观点

- **可迭代评测**：内置评测体系，可量化优化prompt、切片、检索策略

---

## 📝 开源说明

- 授权协议：MIT License

- 使用限制：仅学习、商用二次开发请保留项目开源署名

- 迭代计划：后续规划新增本地Embedding、私有化大模型对接、前端管理页面、异步任务队列

如有定制开发、适配私有化大模型、部署答疑需求，可提交Issues沟通


