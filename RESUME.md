# BOSS 直聘简历 - 项目经验文案

---

## 🚁 智能无人机租赁全栈平台

**2024.09 - 2024.12** | 个人毕业设计项目 | 独立开发

**项目地址**：https://github.com/SpringAI1/ai-drone-rental

### 📋 项目简介

基于 **Spring Boot 3 + Vue 3 + UniApp + Spring AI** 的全栈智能无人机租赁电商平台，覆盖 **Web 管理端、Web 用户端、跨端小程序** 三端同源。实现从注册→资质审核→浏览→下单→支付→发货→收货→归还→评价→报修→维修的**完整业务闭环**。集成 **Spring AI Agent + RAG 知识库 + WebSocket 实时通知**，打造智能化租赁体验。

### 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 · Spring AI 1.0.0 · Spring WebSocket · Spring Cache |
| ORM | MyBatis Plus 3.5 + SQLite（开发）/ MySQL（生产） |
| 安全 | BCrypt 密码哈希 · JWT 鉴权（jjwt）· 角色分级拦截器 · RSA+AES 信封加密 · 登录速率限制 |
| 缓存 | Redis 7 + Jackson 多态类型序列化 |
| AI | 通义千问 LLM（OpenAI 兼容协议）· Agent + Tool Calling · RAG 向量知识库 · TokenTextSplitter |
| 前端 | Vue 3.4 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + SCSS |
| 跨端 | UniApp (Vue 3 + UTS) — H5 / 微信小程序 / iOS / Android |
| 实时通信 | WebSocket + HandshakeInterceptor（解决 token 丢失） |
| 工具链 | Maven 3.9 · Java 17 · Node 20 · Git · Swagger 3 · Lombok · Hutool · Druid |

### 💡 核心亮点

1. **AI 智能客服**：三级回复策略（本地 RAG 知识库 < 100ms → 通义千问 LLM ~1.5s → 兜底模板），支持 Agent 工具调用（无人机推荐/订单查询/维修查询），对话自动审计
2. **知识库管理**：支持 PDF/Word/PPT/Excel/TXT/MD 上传 → Tika 解析 → TokenTextSplitter 切块 → Embedding 向量化 → SQLite 持久化，语义检索按用户可见性过滤
3. **RSA+AES 信封加密**：前端请求体 AES 加密 + RSA 加密传输 AES 密钥，后端 Filter 拦截解密，响应同样加密返回
4. **WebSocket 实时推送**：订单/评论/故障/空域备案多端实时通知，通过 HandshakeInterceptor 在 HTTP 升级阶段将 JWT token 注入 Session，解决 `session.getUri()` 丢失 query string 导致 token 为 null 的经典问题
5. **完整订单状态机**：10 种状态（待支付→已支付→已发货→待收货→租赁中→已归还/已取消/已退款），含库存回滚、自动退款、资质拦截
6. **安全加固**：BCrypt 密码哈希 + 自动 MD5 迁移 · SQL 注入防护（MyBatis Plus Page 分页） · IP 级登录速率限制 · CORS 精确白名单

### 📊 项目规模

- **13 张业务表**（用户/设备/订单/支付/评论/资质/空域备案/故障报修/维修工单/通知/库存日志/知识库文档/知识库片段）
- **60+ REST API 接口**（Swagger 文档）
- **15 个 UniApp 页面**（首页/设备/订单/资质/空域/报修/知识库/AI客服/个人中心）
- **Web 管理端 13 个页面**（仪表盘/订单/设备/用户/资质/评论/空域/报修/知识库/AI审计/个人中心）
- **9 个 Spring AI Agent 工具方法**（智能下单/设备推荐/订单查询/维修查询/状态获取等）

### 🏆 技术难点攻克

- **Spring AI Agent + Tool Calling**：LLM 异步线程 ThreadLocal 丢失问题 → 自定义 ToolContext 透传 userId/username/role
- **Redis 缓存 IPage 反序列化**：MyBatis Plus IPage 无默认构造函数 → 自定义 Jackson 多态类型序列化器
- **WebSocket Token 鉴权**：Spring WebSocket `session.getUri()` 握手后丢失 query string → HandshakeInterceptor 注入 Attributes
- **前端 AES 加密 小程序兼容**：crypto-js + jsencrypt 在小程序环境受限 → 自动降级明文传输

---

**简历一句话总结**：「独立开发的全栈电商平台，覆盖 Spring Boot + Vue3 + UniApp + Spring AI，实现 AI 客服、知识库 RAG、端到端加密、WebSocket 实时推送，60+ API 接口，15 个跨端页面，完整业务闭环。」
