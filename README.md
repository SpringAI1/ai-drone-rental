<div align="center">

# 🚁 AI 无人机租赁平台

**Spring Boot 3 · Vue 3 · UniApp · Spring AI**

[GitHub 主页](https://github.com/SpringAI1) · [项目地址](https://github.com/SpringAI1/ai-drone-rental) · 给我一个 ⭐

</div>

---

## 👨‍💻 关于我（GitHub 主页简介）

- 🎓 **2025 届应届毕业生** · 热爱全栈开发与 AI 应用
- 🚁 独立完成 AI 无人机租赁全栈项目（13 张业务表、60+ 接口、三端同源）
- 🧠 正在深耕 **Spring AI / Agent / RAG** 应用层
- 📫 欢迎合作交流：[GitHub @SpringAI1](https://github.com/SpringAI1)

### 🛠 技术栈

| 方向 | 技能 |
|------|------|
| **后端** | Java 21、Spring Boot 3.2、Spring AI、MyBatis Plus、MySQL 8、Redis、WebSocket、JWT、Swagger、Maven |
| **前端 Web** | Vue 3、Vite 5、Element Plus、Pinia、Vue Router 4、Axios、TypeScript、SCSS |
| **跨端** | uni-app (Vue 3 + UTS)、H5 / 微信小程序 / iOS · Android |
| **AI** | Spring AI、Agent + Tool Calling、RAG 知识库、阿里云通义千问、OpenAI 兼容协议 |
| **工程化** | Git、Docker、Nginx、Vite Proxy、JWT 鉴权、Swagger 文档生成 |

---

## 📋 项目简介

基于 **Spring Boot 3 + Vue 3 + UniApp + Spring AI** 的智能无人机租赁全栈平台，覆盖用户端 Web、跨端小程序与管理端 Web。

**核心能力**：
- 🚀 **完整业务闭环**：注册/资质审核 → 浏览 → 下单 → 支付 → 发货 → 收货 → 归还 → 评价
- 🤖 **AI 智能客服**：三级回复（本地知识库 → 通义千问 LLM → 兜底模板）
- 🔔 **WebSocket 实时通知**：订单/通知/AI 消息多端实时推送
- 📱 **三端同源**：H5 / 微信小程序 / App 共用同一套后端 API
- 📊 **管理后台**：订单、设备、用户、资质、维保、AI 对话审计全覆盖

---

## 🛠 技术栈

**后端**：Spring Boot 3.2 · MyBatis Plus 3.5 · MySQL 8 · Redis · Spring AI · WebSocket · JWT
**Web**：Vue 3 · Vite 5 · Element Plus · Pinia · TypeScript · SCSS
**跨端**：uni-app (Vue 3 + UTS)
**AI**：Spring AI · Agent + Tool Calling · 通义千问 Dashscope · RAG

---

## 🚀 快速开始

```bash
# 1. 数据库
mysql -u root -p < backend/sql/init.sql

# 2. 后端
cd backend && mvn spring-boot:run          # → http://localhost:8080/api

# 3. Web 端
cd frontend-web && npm install && npm run dev   # → http://localhost:3000

# 4. UniApp (HBuilderX 打开 frontend-uniapp，运行到浏览器)
```

🔑 **默认账号**：`admin / 123456`（管理员） · `lisi / 123456`（已通过资质审核）

---

## 📡 API 模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `/auth` | 登录/注册/JWT |
| 用户 | `/user` | 资料/资质/充值/订单 |
| 无人机 | `/drone` | 列表/详情/评价 |
| 订单 | `/order` | 创建/支付/取消/收货/退款 |
| 评价 | `/comment` | 发表/查询/删除 |
| 空域备案 | `/airspace` | 提交/审核 |
| 故障报修 | `/fault` | 提交/审核 |
| 消息通知 | `/notification` | 列表/已读 + WebSocket |
| AI 客服 | `/ai/v2` | 对话/知识库/状态 |
| 文件上传 | `/common/upload` | 头像/图片 |
| 管理端 | `/admin/*` | 仪表盘/订单/设备/审计 |
| 公开 | `/public/stats` | 首页统计（免登录） |

完整文档：`http://localhost:8080/api/swagger-ui/index.html`

---

## 🤖 AI 智能客服（三级回复）

```
用户问题
   ↓
1️⃣ 本地知识库（< 100ms）→ 命中直接返回
   ↓ 未命中
2️⃣ 通义千问 LLM（~1.5s）→ 自然语言回复
   ↓ 失败
3️⃣ 兜底模板 → 引导用户重试
   ↓
自动保存到数据库，供管理端审计
```

支持 Agent **工具调用**：无人机推荐 / 订单查询 / 维修记录查询。

---

## 📊 订单状态机

```
待支付(0) → 待发货(1) → 待收货(2) → 租赁中(3) → 已归还(4)
    ↘             ↘            ↘
    已取消(5)    已退款(6)
```

- 取消已支付订单 → **自动退款 + 库存回滚**
- 资质未通过 → **禁止下单**

---

## 🗺 路线图

- [x] v1.0 — 基础租赁
- [x] v2.0 — 订单状态分类 + 评价
- [x] v3.0 — AI 客服 + 空域备案 + 故障报修 + 实时通知
- [ ] v4.0 — 真实支付 + 地图定位 + 信用评分

---

## 📄 License

MIT — 自由使用、修改、分发，请保留版权声明。

---

<div align="center">

**如果觉得有帮助，请给一个 ⭐ Star！**

Made with ❤️ by [SpringAI1](https://github.com/SpringAI1)

</div>
