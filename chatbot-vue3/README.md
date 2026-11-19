# 小智 AI 前端

目录：`chatbot-vue3`。作者：lazysheep101。

这一层是三个页面入口：买家咨询、内部测试台、商家后台。业务规则在后端。分层说明见仓库根目录 [架构说明.md](../架构说明.md)，本层的页面和接口对照见 [docs/层/使用层.md](../docs/层/使用层.md)。

## 环境

- Node.js 18+
- 后端已经在 `http://localhost:8081` 启动

## 启动

```powershell
npm install
npm run dev -- --port 5174
```

浏览器打开 `http://localhost:5174`。

`vite.config.js` 把以 `/api` 开头的请求转到 `http://localhost:8081`，并去掉 `/api`。同时跑博客时不要占用博客的 `5173`，所以这里写明 `5174`。

生产包：

```powershell
npm run build
```

产物在 `dist/`。这个前端不带后端，部署时要另有一层把 `/api` 转到 Spring Boot。

## 打开哪个地址

| 地址 | 页面 |
|------|------|
| `http://localhost:5174/#/widget` | 买家咨询 |
| `http://localhost:5174/#/` | 内部测试台 |
| `http://localhost:5174/#/admin/login` | 商家登录 |

商家账号：`admin` / `123456`（可审批），`supervisor` / `super123`（可审批），`agent` / `agent123`（只处理收件箱）。

## 代码在哪

| 路径 | 内容 |
|------|------|
| `src/router/index.js` | Hash 路由。没登录不能进 `/admin` |
| `src/views/widget/WidgetView.vue` | 买家卡片、SSE、人工回复轮询 |
| `src/views/Index.vue`、`ChatPage.vue`、`CustomerServiceChatPage.vue` | 测试台 |
| `src/views/admin/` | 后台各页。壳是 `AdminLayout.vue` |
| `src/api/admin.js` | 后台接口。自动带 `Authorization` |
| `src/axios.js` | `baseURL` 为 `/api`。401 时清 Token 并回到登录页 |

买家对话的 SSE 地址是 `/api/customer-service/chat/completion`，不经过 `admin.js`。
