# spring-ai-crossborder-chatbot · 电商客服 Agent 改造说明

> 状态：计划内改造已完成（底座 RAG 保留，叠加业务 Tool Agent + 简易后台；待本机 JDK21 + PG 联调验收）  
> 项目名称：`spring-ai-crossborder-chatbot` · Author：`lazysheep101`  
> 文档位置：`spring-ai-crossborder-chatbot/docs/电商客服Agent改造说明.md`  
> **每次修改先参考**：`view-work/spring-ai-crossborder-customer-service`（OmniMerchant，只读；上游 [spring-ai-crossborder-customer-service](https://github.com/RyanCoreAI/spring-ai-crossborder-customer-service)）

下文记录的是已经落地的边界。以后改 Tool、审批、分流、转人工或商家后台，仍然先打开该参考仓看同一类能力怎么做，再改本仓。

1. 对照参考仓的受控 Agent：意图分流、工具白名单、高风险只建待审单、Evidence / 引用，不直接改账。
2. 本仓保留混合 RAG 底座（PGVector + ES + RRF + DashScope Rerank），不换成参考仓的检索实现。
3. 参考仓只读。吸收边界和模式，不整仓搬运，不把 OmniMerchant 代码整段拷进来。

---

## 1. 主方向与业务边界（已敲定）

### 一句话定位

以现有 **Spring AI 混合 RAG 客服** 为底座，升级为 **小智AI**（`spring-ai-crossborder-chatbot`）智能客服 Agent：政策/FAQ 走检索，订单/物流/商品走工具查库；高风险写操作只建待审批单，不直接改账。

### 做（In）

| 能力 | 说明 |
|------|------|
| 混合 RAG 底座 | PGVector + ES + RRF + DashScope Rerank（保留） |
| 政策工具化 | `refundPolicyRAG` 调用混合检索，避免与 Advisor 双重召回 |
| 业务只读工具 | 查订单、查物流、搜商品/库存 |
| 受控写工具 | 退货/退款/换货/改地址 → `PENDING` 审批请求 |
| 意图分流 | 精简 specialist + 工具白名单 |
| 转人工 | `escalateToHuman` + 工单 |
| 工具审计 | `t_tool_call_log` |
| 商家后台（最小） | 登录、订单、审批、工单、工具日志、知识库、RAG Debug |
| 模式开关 | `rag.agent.enabled`：Agent ↔ 原 Advisor RAG 互斥 |

### 不做（Out）

- 完整商城前台 / 支付 / 秒杀 / 营销中台
- 抖店 / Shopify / 企微全渠道真连接（可后续 Fixture）
- 多租户 SaaS、计费大盘
- GraphRAG、多 Agent 集群编排
- 仓储 WMS / PLC（同架构可另开 Tool 包，非本迭代主线）
- LLM 直接执行退款到账

---

## 2. 架构关系

```text
用户（Vue 客服页）
    │  SSE /chat/completion
    ▼
AiCustomerServiceController
    ├─ rag.agent.enabled=false → CustomerServiceAdvisor（原混合 RAG）
    └─ rag.agent.enabled=true  → AgentChatService
            │
            ├─ AgentOrchestratorService（关键词分流 + 工具白名单）
            ├─ ChatClient + ToolCallback（Order/Logistics/Product/Policy/Escalation）
            │       ├─ 读：CommerceService → PostgreSQL 业务表
            │       └─ 政策：HybridRetrievalService → 向量/ES/RRF/Rerank
            └─ ToolAuditService → t_tool_call_log

商家后台（Vue /admin）
    └─ AdminCommerceController → 订单 / 审批 / 工单 / 工具日志 / 登录
```

**原则：**

1. **RAG 与 Agent 互斥挂载**：Agent 开启时不挂 `CustomerServiceAdvisor`，政策检索只走 `refundPolicyRAG`，避免双重召回占上下文。  
2. **读自动、写审批**：写工具只落 `t_commerce_action_request`。  
3. **身份校验**：查单/退换货工具要求订单号 + 邮箱或手机号。

---

## 3. 代码与表结构落点（当前仓库）

### 后端包

| 包/文件 | 职责 |
|---------|------|
| `agent/AgentChatService` | Agent 流式对话入口 |
| `agent/AgentOrchestratorService` | 意图分流 + 工具白名单 |
| `agent/tool/*` | `@Tool`：订单/物流/商品/政策/升级 |
| `agent/ToolAuditService` | 工具调用审计 |
| `agent/config/ToolCallbackConfig` | 注册 ToolCallbackProvider |
| `commerce/*` | 业务服务、审批、管理员鉴权、Schema 初始化 |
| `helpdesk/*` | 工单相关 |
| `controller/AiCustomerServiceController` | 客服对话双模式切换 |
| `controller/AdminCommerceController` | 后台 API |
| `eval/AgentContractCase` | 契约用例（评测向） |
| `resources/sql/commerce_agent.sql` | 业务表 + 种子数据 |
| `config/RagProperties.Agent` | `enabled` + systemPrompt |

### 前端

| 路径 | 职责 |
|------|------|
| `views/admin/LoginView.vue` | 后台登录 |
| `views/admin/OrdersView.vue` | 订单 |
| `views/admin/ActionsView.vue` | 高风险动作审批 |
| `views/admin/TicketsView.vue` | 工单 |
| `views/admin/ToolLogsView.vue` | 工具日志 |
| `views/admin/KnowledgeView.vue` | 知识库列表、分片上传、删除 |
| `views/admin/RagDebugView.vue` | RAG 工作台（`/admin/rag-workbench`） |
| `views/admin/ConversationsView.vue` | 会话记录 |
| `views/admin/EvalsView.vue` | 契约评测摘要 |
| `views/admin/AgentWorkflowView.vue` | specialist 白名单 |
| `views/admin/MultilingualView.vue` | 语种与翻译回退证据 |
| `views/admin/AuditView.vue` | 审批 / 工具 / 收件箱审计 |
| `views/admin/UsersView.vue` | 演示账号说明 |
| `views/widget/WidgetView.vue` | 买家咨询组件 |
| `api/admin.js` | 后台 API 封装 |

### 核心表（PostgreSQL）

- `t_customer` / `t_product` / `t_order` / `t_order_item` / `t_shipment`
- `t_commerce_action_request`（待审动作）
- `t_tool_call_log`
- `t_ticket`
- `t_admin_user`

启动时由 `CommerceSchemaInitializer` 幂等执行 `commerce_agent.sql` 并写演示种子（以代码为准）。

---

## 4. 工具清单

| Tool 名 | 类型 | 行为 |
|---------|------|------|
| `queryOrder` | 读 | 订单详情，校验联系方式 |
| `trackLogistics` | 读 | 物流轨迹 |
| `searchProductCatalog` | 读 | 商品/库存检索 |
| `refundPolicyRAG` | 读/RAG | 混合检索政策片段 + citations |
| `createReturnRequest` | 写→审 | 创建退货 PENDING |
| `requestRefundOrReplacement` | 写→审 | 退款/换货 PENDING |
| `requestAddressChange` | 写→审 | 改地址 PENDING |
| `escalateToHuman` | 工单 | 转人工 |

分流示例（见 `AgentOrchestratorService`）：退货退款 → return 专家；物流 → order；商品 → product；政策 → policy_rag；投诉/转人工 → handoff。

---

## 5. 配置

`application.yml`：

```yaml
rag:
  agent:
    enabled: true   # true=Tool Agent；false=原 Advisor RAG
```

System Prompt 默认可在 `RagProperties.Agent.systemPrompt` / 配置覆盖，核心规则：

1. 必须用工具，禁止编造订单/物流/政策  
2. 查单与退换货校验身份  
3. 写操作只创建待审，禁止声称已退款成功  
4. 政策必须调 `refundPolicyRAG`  
5. 激烈情绪或明确要求 → `escalateToHuman`

---

## 6. 演示话术（建议）

1. 「ORD-xxxx 到哪了？我的邮箱是 xxx」→ 查单 + 物流  
2. 「卫衣还有货吗」→ 商品目录  
3. 「七天无理由怎么退」→ `refundPolicyRAG` + 引用  
4. 「这单要退货」→ 建 PENDING，后台 `Actions` 审批  
5. 「我要投诉转人工」→ 工单  

（具体订单号/邮箱以种子数据为准，启动后查库或后台订单页。）

---

## 7. 参考项目（改造对标，非依赖）

### 电商

| 仓库 | 吸收点 |
|------|--------|
| [RyanCoreAI/spring-ai-crossborder-customer-service](https://github.com/RyanCoreAI/spring-ai-crossborder-customer-service) | 受控 Agent、审批门、Evidence RAG、工具白名单 |
| [Codes-tutorials/spring-ai-customer-service](https://github.com/Codes-tutorials/spring-ai-customer-service) | 最小 Function Calling（订单/库存） |
| [dominikcebula/spring-ai-shopping-agent](https://github.com/dominikcebula/spring-ai-shopping-agent) | MCP 拆商品/订单 |
| [tttdddg/Smart-Commerce-Agent](https://github.com/tttdddg/Smart-Commerce-Agent) | mall 上挂 Tool |
| [sxt12356/mini-agent](https://github.com/sxt12356/mini-agent) | 售后 + HITL 审批边界 |

### 仓储（同架构第二套 Tool，非本迭代）

| 仓库 | 吸收点 |
|------|--------|
| [ly1836/spring-ai-rag-demo](https://github.com/ly1836/spring-ai-rag-demo) | ERP/仓储 Tools + RAG |
| [jmix-edu/ai-warehouse](https://github.com/jmix-edu/ai-warehouse) | 库存/补货/预留工具边界 |

---

## 8. 演进顺序（简历故事线）

1. **答得准**：混合 RAG + 政策 citations（底座已有，Agent 内工具化）  
2. **办得了**：订单/物流/商品 Tool + 写操作审批  
3. **像产品**：商家后台审批/工单/工具日志 + `rag.agent.enabled` 开关  
4. **已补**：契约评测（含 PENDING 门禁）、引用卡、双语 FAQ、后台上传。Docker Compose / MCP / weblog 入库仍不在本链路。  

---

## 9. 协作说明

- 本仓库可能有其他会话同步改 Agent/后台代码；**以源码与 SQL 为准**，本文描述目标边界与当前主结构。  
- 若表结构、Tool 名、后台路由有变更，请同步更新本文第 3～5 节。  
- 切换旧链路验证：设 `rag.agent.enabled=false` 后走原 `CustomerServiceAdvisor`。

---

## 10. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-10-29 | 初版：确认此前无改造 MD；按已落地代码与敲定边界补写本文 |
| 2026-11-17 | 产品名统一为小智AI；补会话/评测/工作流/审计/账号/多语言页与后台知识库上传 |
