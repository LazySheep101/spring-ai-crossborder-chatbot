# 小智 AI · 跨境电商智能客服

> Spring AI 混合检索 + 受控工具 Agent + 商家运营后台 · By [LazySheep101](https://github.com/LazySheep101)

买家咨询、政策检索和订单工具走同一条客服链路。高风险退换货只创建待审批单。商家后台处理收件箱、审批、知识和轨迹回放。

## 先读哪份文档

| 顺序 | 文档 | 内容 |
|------|------|------|
| ① | [架构说明.md](架构说明.md) | 有哪些层，一条消息怎么走 |
| ② | [docs/层/使用层.md](docs/层/使用层.md) 起 | 每一层的页面 / 类 / 接口 / 表 |
| ③ | [chatbot-springboot/README.md](chatbot-springboot/README.md) | 后端怎么启动、包结构 |
| ④ | [chatbot-vue3/README.md](chatbot-vue3/README.md) | 前端三端、路由、代理 |
| ⑤ | [docs/README.md](docs/README.md) | docs 目录里每份专项说明干什么 |
| ⑥ | [docs/演示入口说明.md](docs/演示入口说明.md) | 5 分钟演示剧本 |
| ⑦ | [docs/数据库初始化说明.md](docs/数据库初始化说明.md) | PostgreSQL / pgvector / SQL |

| 子目录 | 说明 | 技术 |
|--------|------|------|
| [`chatbot-springboot`](./chatbot-springboot) | 后端 | Spring Boot、Java 21、Spring AI、PostgreSQL/pgvector、Elasticsearch |
| [`chatbot-vue3`](./chatbot-vue3) | 前端 | Vue 3、Vite、Ant Design Vue |
| [`docs`](./docs) | 分层与专项说明 | 入口、数据库、改造边界 |

演示账号（启动后由种子数据写入，密码会升成 BCrypt）：

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `123456` | 主管，可审批 |
| `supervisor` | `super123` | 主管，可审批 |
| `agent` | `agent123` | 客服，可处理收件箱 |

## 快速开始

### 环境要求

- JDK **21**、Maven **3.8+**
- Node.js **18+**
- PostgreSQL（需 pgvector），库名 `robot`
- Elasticsearch（可关混合检索后只用向量）
- 对话模型与向量模型的 API Key（写在本机配置，不要提交密钥）

### 后端

```bash
cd chatbot-springboot
# 按仓库内 example / 本地 application-dev.yml 配置数据库与模型 Key
mvn spring-boot:run
```

默认端口 **8081**。与商城同时跑时可设 `SERVER_PORT=8082`，并改前端 Vite 代理。

### 前端

```bash
cd chatbot-vue3
npm install
npm run dev -- --port 5174
```

Vite 把 `/api` 转到 `http://localhost:8081` 并去掉 `/api` 前缀。

- 买家：`http://localhost:5174/#/widget`
- 测试台：`http://localhost:5174/#/`
- 商家后台：`http://localhost:5174/#/admin/login`

## License

仅供学习与交流。参考或衍生请保留原作者信息。

## 联系

- GitHub：[@LazySheep101](https://github.com/LazySheep101)
