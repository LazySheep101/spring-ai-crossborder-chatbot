# 小智 AI 后端

目录：`chatbot-springboot`。作者：lazysheep101。

一个 Spring Boot 进程，包名 `com.quanxiaoha.ai.robot`。不拆微服务。分层总览在仓库根目录 [架构说明.md](../架构说明.md)。

## 环境

- JDK 21
- Maven 3.8+
- PostgreSQL，库名 `robot`，已安装 pgvector
- Elasticsearch，本地默认 `http://localhost:9200`

把 `src/main/resources/application-dev.yml.example` 复制成 `application-dev.yml`，填数据库密码和模型 Key。真实的 `application-dev.yml`、`application-prod.yml` 不要提交。公开仓库只保留 `*.example`。

## 启动

在本目录执行：

```powershell
mvn spring-boot:run
```

`application.yml` 里端口是 **8081**，激活的 profile 是 `dev`。启动时幂等执行 `sql/ai_robot.sql` 和 `sql/commerce_agent.sql`。

本机若默认 `java` 不是 21，先把这次命令的 `JAVA_HOME` 指到 JDK 21，不要改全局 Java 配置也可以。

不跑需要数据库的测试、只跑契约和安全门：

```powershell
mvn "-Dtest=AgentSideChannelTests,SafetyGateAndCommerceEvalTests,AgentContractEvalTests" test
```

## 包和层

| 包 | 层 | 说明 |
|----|----|------|
| `controller` | 接入 | [docs/层/接入层.md](../docs/层/接入层.md) |
| `agent` | 编排 | [docs/层/编排层.md](../docs/层/编排层.md) |
| `commerce` | 交易 | [docs/层/交易层.md](../docs/层/交易层.md) |
| `helpdesk` | 人工 | [docs/层/人工层.md](../docs/层/人工层.md) |
| `i18n` 与 `service` | 检索和语言 | [docs/层/检索与语言层.md](../docs/层/检索与语言层.md) |
| `domain` | 数据 | 表实体和 Mapper，见 [docs/层/数据层.md](../docs/层/数据层.md) |
| `eval` | 评测 | 不在买家请求的主链路上 |

买家接口前缀 `/customer-service`，不校验后台 JWT。后台接口前缀 `/admin`，除 `POST /admin/auth/login` 外都要 JWT。

后端不托管 Vue。页面在 `chatbot-vue3`，开发时由 Vite 把 `/api` 转到本进程并去掉前缀。
