# 小智 AI · 跨境电商智能客服

> Spring AI 混合检索 + 受控工具 Agent + 商家运营后台 · By [LazySheep101](https://github.com/LazySheep101)

买家咨询、政策检索和订单工具走同一条客服链路。高风险退换货只创建待审批单。商家后台处理收件箱、审批、知识和轨迹回放。

| 子目录 | 说明 | 技术 |
|--------|------|------|
| [`chatbot-springboot`](./chatbot-springboot) | 后端 | Spring Boot、Java 21、Spring AI、PostgreSQL/pgvector、Elasticsearch |
| [`chatbot-vue3`](./chatbot-vue3) | 前端 | Vue 3、Vite、Ant Design Vue |
| [`docs`](./docs) | 说明 | 入口、数据库、改造边界 |

演示账号（启动后由种子数据写入，密码会升成 BCrypt）：

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `123456` | 主管，可审批 |
| `supervisor` | `super123` | 主管，可审批 |
| `agent` | `agent123` | 客服，可处理收件箱 |

---

## 快速开始

### 环境要求

- JDK **21**、Maven **3.8+**
- Node.js **18+**
- PostgreSQL（需 pgvector）
- Elasticsearch（可关混合检索后只用向量）
- 对话模型与向量模型的 API Key（自行申请）

### 后端

```bash
cd chatbot-springboot

cp src/main/resources/application.yml.example \
   src/main/resources/application.yml

cp src/main/resources/application-dev.yml.example \
   src/main/resources/application-dev.yml

cp src/main/resources/application-prod.yml.example \
   src/main/resources/application-prod.yml
```

填写数据库密码、JWT 密钥、对话 / 向量 / 重排的 API Key。创建库 `robot` 后可手工执行：

```text
chatbot-springboot/src/main/resources/sql/init_all.sql
```

应用启动时也会幂等建表。

```bash
mvn spring-boot:run
```

### 前端

```bash
cd chatbot-vue3
npm install
npm run dev
```

| 入口 | 地址 |
|------|------|
| 买家咨询 | `http://localhost:5173/#/widget` |
| 商家后台 | `http://localhost:5173/#/admin/login` |
| 内部测试台 | `http://localhost:5173/#/` |

---

## 配置说明

仓库只提交 `*.example`。下面这些不要提交：

- `application.yml` / `application-dev.yml` / `application-prod.yml`
- 数据库密码、模型 API Key、JWT 密钥
- 本机文件目录

---

## License

仅供学习与交流。
