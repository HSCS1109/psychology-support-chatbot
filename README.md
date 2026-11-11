# 智能心理关怀系统（AI Mental Health Assistant）

基于 Spring AI 与 Vue 3 的全栈心理健康助手，通过 SSE 流式对话提供情绪支持，并具备意图识别、工具调用、危机干预与端到端评测能力。

## 功能特性

- **流式心理咨询对话**：基于 Spring AI ChatClient 与 Project Reactor，以 SSE（Server-Sent Events）逐字输出回复。
- **意图识别**：规则层 + 大模型层两层识别，按意图动态路由（危机 / 工具 / 正常咨询等）。
- **工具调用**：基于 Spring AI Function Calling，按需调用心理量表、求助热线等工具。
- **安全护栏与危机干预**：识别自伤、自杀等表达，触发固定求助话术并引导专业资源。
- **多轮记忆**：会话历史与摘要式记忆，支持上下文连续对话。
- **情绪日记**：记录与查看情绪状态。
- **心理量表评估**：内置量表工具与结果解读。
- **知识库**：心理科普文章、分类、收藏，含前台浏览与后台管理。
- **文件上传**：本地存储与访问。
- **数据统计**：后台数据看板与 ECharts 可视化。
- **认证鉴权**：Spring Security + JWT，支持 access / refresh 双 token。
- **端到端评测**：提供自动化评测、意图准确率、trace 回放与 LLM 评审脚本。

## 技术栈

**后端**

- Java 17、Spring Boot 3.5.14
- Spring AI 1.1.5（OpenAI 兼容接口，接入硅基流动 DeepSeek-V3）
- Spring Security、java-jwt 4.4.0
- MyBatis-Plus 3.5.7
- MySQL、Hutool 5.8.25、Lombok、Project Reactor

**前端**

- Vue 3、Vite 8、Vue Router、Pinia
- Element Plus、Axios、ECharts、wangEditor
- @microsoft/fetch-event-source（SSE 客户端）

## 目录结构

```
code/
├── ai-springboot/          # 后端（Spring Boot）
│   └── src/main/java/org/example/aispringboot/
│       ├── AiService/      # 智能体核心：编排、意图、工具、记忆、安全护栏等
│       ├── controller/     # 接口层
│       ├── service/        # 业务层
│       ├── mapper/         # 数据访问层（MyBatis-Plus）
│       ├── entity/         # 实体
│       ├── config/         # 配置（ChatClient、Security、JWT 等）
│       └── ...
├── ai-vue/                 # 前端（Vue 3 + Vite）
│   └── src/
│       ├── views/          # 页面
│       ├── components/     # 组件
│       ├── api/            # 接口封装
│       ├── router/、stores/
│       └── ...
└── eval/                   # 评测脚本
    ├── run_eval.py         # 端到端评测（通过率等指标）
    ├── test_intent.py      # 意图识别准确率
    ├── replay_trace.py     # 调用链 trace 回放
    └── llm_judge.py        # LLM-as-Judge 主观质量评审
```

## 快速开始

### 环境要求

- JDK 17+
- Maven（或使用项目自带 mvnw）
- Node.js 18+、npm
- MySQL 5.7+ / 8.0

### 1. 数据库准备

- 创建数据库 `mental_health_assistant`。
- 按后端实体建立对应表。
- 注意：本仓库未包含建表 SQL，需自行准备表结构。

### 2. 后端配置与启动

- 将配置模板 `src/main/resources/application-example.yml` 复制为同目录下的 `application.yml`。
- 在 `application.yml` 中填写：数据库用户名 / 密码、大模型 `api-key`、JWT `secret`。
- 启动方式：
  - IDEA 中运行 `AiSpringbootApplication`；
  - 或命令行（仓库根目录）执行 `.\mvnw.cmd spring-boot:run`。
- 后端默认端口 `1236`，启动成功标志：日志出现 `Started AiSpringbootApplication`。

### 3. 前端配置与启动

```
cd ai-vue
npm install
npm run dev
```

- 前端默认端口 `5173`，已通过 Vite 将 `/api` 代理转发到后端 `1236`。

## 配置说明

- 真实 `application.yml` 已被 `.gitignore` 忽略，不会提交；仓库仅提供 `application-example.yml` 占位模板。
- 大模型采用 OpenAI 兼容协议，可通过 `base-url` 与 `model` 切换服务和模型。
- 评测脚本 `llm_judge.py` 从环境变量读取密钥，PowerShell 下运行前设置：

```
$env:SILICONFLOW_API_KEY="你的key"
```

## 端口说明

| 服务 | 端口 |
| --- | --- |
| 后端 | 1236 |
| 前端 | 5173 |
| MySQL | 3306 |

## 免责声明

本系统输出由 AI 生成，仅供心理科普与情绪支持参考，不能替代专业心理医生的诊断与治疗。如遇紧急心理危机，请立即拨打当地心理援助热线或前往专业医疗机构。
