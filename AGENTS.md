# XApi 项目开发规范

本文件是本项目的强制开发规范。所有人工开发者和 agent 在执行代码、配置、测试、提交、合并、推送等动作前，都必须先阅读并遵守本文。

## 1. 项目结构规范

### 根目录

- `src/main/java/com/example/xapi`：Spring Boot 后端源码。
- `src/test/java/com/example/xapi`：后端测试。
- `frontend`：React + Vite + TypeScript 前端。
- `agents/stock-agent`：Python FastAPI 股票 Agent。
- `agents/stock-agent/vendor/TradingAgents`：外部 TradingAgents 项目源码，只作为 vendored dependency 使用。
- `application.yml.example`：可提交的配置样例。
- `src/main/resources/application.yml`：默认配置，只允许放可公开默认值，敏感信息必须使用环境变量。
- `docker-compose.yml`：本地基础服务，如 Redis。
- `target/`、`frontend/dist/`、`frontend/node_modules/`、`__pycache__/`、`*.pyc`、`*.log`：生成物或本地运行产物，禁止提交。

### 后端结构

- `api`：Controller、异常处理器、HTTP API 入口。
- `service`：业务编排逻辑，负责缓存、上游调用、降级策略、校验。
- `dto`：接口输入输出数据结构。
- `config`：配置属性、Bean 配置。
- `cache`：Redis 缓存抽象和实现。
- `upstream`：X API 等外部服务客户端和解析器。

后端不得把 HTTP 解析、业务编排、缓存细节和上游协议混在同一个类里。

### 前端结构

- `frontend/src/components/layout`：整体布局、侧边栏、右侧栏。
- `frontend/src/components/feed`：推文列表、推文卡片、推文详情。
- `frontend/src/components/agent`：股票 Agent UI。
- `frontend/src/components/common`：通用提示、状态组件。
- `frontend/src/components/ui`：基础 UI 组件。
- `frontend/src/hooks`：数据请求、主题、翻译、Agent 等状态逻辑。
- `frontend/src/types`：前端类型定义。

布局边界必须清晰：主内容区展示推文列表和推文详情；右侧栏展示状态、缓存、限流、Agent 等辅助工具。股票 Agent 必须放在右侧栏，不得放回主内容区。

### Python Agent 结构

- `agents/stock-agent/app/main.py`：FastAPI 入口。
- `agents/stock-agent/app/agent_service.py`：Agent 主编排逻辑。
- `agents/stock-agent/app/market_tools.py`：股票代码识别和行情查询。
- `agents/stock-agent/app/post_tools.py`：X 用户帖子读取。
- `agents/stock-agent/app/models.py`：Pydantic 模型。
- `agents/stock-agent/tests`：Python Agent 测试。

Python Agent 查询股票必须使用 Python 实现。Java 后端只做代理和配置，不直接承担股票行情查询逻辑。

## 2. 代码规范

### 通用规范

- 修改前必须先读相关现有代码，遵循已有分层和命名风格。
- 优先做小范围、低耦合改动；禁止顺手重构无关模块。
- 中文文案必须是正常 UTF-8，不得出现乱码。
- 敏感信息不得写入源码、配置样例、日志或提交记录。
- 错误处理必须面向用户可理解，不得把内部异常栈直接暴露到前端。
- 新增外部依赖前必须确认现有依赖无法满足，并说明原因。

### 后端规范

- Controller 只处理 HTTP 入参、调用 service、返回 DTO，不写业务逻辑。
- Service 负责业务流程、缓存、降级、异常语义。
- DTO 保持简单，不放业务逻辑。
- 多个同类型 Bean 必须使用 `@Qualifier` 显式注入，例如多个 `RestTemplate`。
- Redis 不可用时，不得让前端只看到重复的“请求出错”；必须尽量走可控降级：
  - 有 stale cache 时返回 stale cache。
  - 无缓存且业务允许时直接请求上游并标记无缓存。
  - 只有在会造成限流、数据污染或安全风险时才拒绝请求。
- X API 相关接口必须尊重限流保护，不得无条件绕过限流。
- 翻译接口必须正确编码 URI，不能把未编码的 `|`、空格、URL 等直接拼进请求地址。

### 前端规范

- 前端必须使用 TypeScript 类型，不得把 API 响应当作 `any` 直接使用。
- 请求逻辑放在 hook 中，组件只负责展示和交互。
- 主内容区只承载主要阅读流：推文列表和推文详情。
- 右侧栏承载辅助信息：当前查询、Agent、限流、缓存说明。
- Agent UI 必须适配右侧栏宽度，使用单列布局，避免挤压主内容区。
- 前端错误提示要避免重复堆叠；同一错误不应在页面上出现多个“请求出错”。
- 按钮、输入框、加载态、空态必须保留，不得为了修布局删除核心交互。
- 修改 UI 后必须用浏览器检查实际页面，不能只依赖构建通过。

### Python Agent 规范

- Agent 的 `/ask` 必须在单个上游失败时继续返回可用结果，并把失败放入 `warnings`。
- 股票行情上游失败时，返回占位 quote，`source` 可为 `unavailable`，不得让整个请求 500。
- 用户帖子上游失败时，保留股票分析结果，并追加 warning。
- `TradingAgents` 已 vendored，但不得假设本地 Python 环境已经安装其所有依赖；不可用时必须降级到轻量工具。
- A股和美股代码识别必须避免互相误判，例如 `.SH`、`.SZ` 不得被识别成美股代码。

## 3. 测试与验证规范

### 必跑验证

- 后端改动：
  - `mvn test`
  - 如只改单个 service，可先跑 `mvn -Dtest=ClassName test`，最终仍需跑 `mvn test`。
- 前端改动：
  - `cd frontend`
  - `npm run build`
  - 涉及 UI 或交互时，必须用浏览器打开本地页面验证。
- Python Agent 改动：
  - `$env:PYTHONPATH='agents\stock-agent'; python -m pytest agents\stock-agent\tests -q`
- 跨端改动：
  - 同时跑相关后端、前端、Python 验证。
  - 至少做一次接口或浏览器端到端检查。

### 测试写法

- 修 bug 必须先补能复现问题的测试，再修实现。
- 测试应覆盖真实行为，不只验证 mock 被调用。
- 对 Redis、X API、行情接口、翻译接口等外部依赖，单测使用 fake/mock transport，不直接依赖公网稳定性。
- 浏览器验证必须确认页面结构、关键文字、按钮行为和错误状态。

## 4. Agent 执行规范

### 开始前

- 必须先运行或查看 `git status --short --branch`。
- 必须确认当前分支和远端，避免在错误分支上改动。
- 必须先搜索和阅读相关代码，不得凭记忆修改。
- 如果工作区存在用户未提交改动，必须保留并绕开；不得覆盖、回滚或清理。

### 执行中

- 改文件前必须说明要改什么和原因。
- 编辑文件优先使用补丁方式，避免用临时脚本大范围重写。
- 不得执行破坏性命令，如 `git reset --hard`、`git checkout -- <file>`，除非用户明确要求。
- 不得把 `node_modules`、`target`、`dist`、日志、缓存、`.pyc` 提交。
- 本地服务端口冲突时，不要杀未知用户进程；优先换端口验证。

### 完成后

- 必须运行对应验证命令并读取结果。
- 必须检查 `git status --short`。
- 必须检查是否误提交生成物。
- 最终回复必须说明：
  - 改了什么。
  - 验证跑了什么。
  - 是否还有外部依赖限制，例如 Redis 未启动、X 凭证缺失、行情接口不可用。

## 5. Git 协作规范

- 功能开发优先使用独立分支，分支名要描述功能，例如 `feature-stock-agent`。
- 提交信息使用简洁英文：
  - `feat: add python stock agent`
  - `fix: move stock agent to right panel`
  - `fix: handle redis unavailable fallback`
- 每个提交应聚焦一个主题，避免把规范、UI、后端 bug、生成物混在一起。
- 提交前必须确认：
  - 工作区没有无关改动。
  - 验证已通过。
  - 没有生成物被 staged。
- 合并到 `main` 前必须：
  - 拉取最新 `origin/main`。
  - 解决冲突时保留双方核心功能。
  - 合并后重新运行完整验证。
- 推送后必须确认远端分支或 `main` 的 commit hash。

## 6. 运行规范

### 本地默认服务

- Redis：
  - `docker compose up -d redis`
- 后端：
  - `mvn spring-boot:run`
  - 默认端口 `8080`
- 前端：
  - `cd frontend`
  - `npm run dev`
  - 默认端口 `5173`
- Python Agent：
  - `cd agents/stock-agent`
  - `python -m uvicorn app.main:app --host 127.0.0.1 --port 9001`

### 端口占用处理

- 如果 `8080` 被占用，可用 `--server.port=18080` 临时验证。
- 如果前端 `5173` 被占用，可用 `npm run dev -- --host 127.0.0.1 --port 5174`。
- 如果临时后端端口不是 `8080`，前端代理要设置 `VITE_API_PROXY_TARGET`。
- 如果 Python Agent 需要调用临时后端，要设置 `XAPI_BASE_URL`。

## 7. 外部依赖与降级

- Redis 是缓存和限流保护组件，但 Redis 不可用不应直接导致所有帖子功能消失。
- X API 需要有效凭证；凭证缺失或过期时，前端应显示明确错误。
- Alpha Vantage 没有 API key 时，美股 quote 可以返回无价格占位结果。
- 东方财富等行情上游临时失败时，Agent 返回 warning，不应整体失败。
- 翻译服务失败时必须尝试 fallback；fallback 也失败时再返回明确错误。

## 8. 禁止事项

- 禁止提交敏感 token、cookie、csrf、guest token。
- 禁止提交 `*.log`、`node_modules`、`dist`、`target`、`__pycache__`、`*.pyc`。
- 禁止在未验证的情况下声称完成。
- 禁止把 Agent 放回主内容区。
- 禁止用乱码文案交付。
- 禁止删除帖子列表、翻译、详情页、财经博主快捷入口等现有核心功能来规避错误。
