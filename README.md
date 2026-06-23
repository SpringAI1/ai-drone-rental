<div align="center">

# 🚁 AI 无人机租赁平台

**Spring Boot 3 · Vue 3 · UniApp · Spring AI · WebSocket**

[![Java](https://img.shields.io/badge/Java-23-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org)
[![UniApp](https://img.shields.io/badge/uni--app-Vue3-2C8EFF?logo=vue&logoColor=white)](https://uniapp.dcloud.net.cn)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io)
[![SQLite](https://img.shields.io/badge/SQLite-3-003B57?logo=sqlite&logoColor=white)](https://sqlite.org)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5-1693E0)](https://baomidou.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[项目主页](https://github.com/SpringAI1/ai-drone-rental) · [Issues](https://github.com/SpringAI1/ai-drone-rental/issues) · 给我一个 ⭐

</div>

---

## 📖 项目简介

基于 **Spring Boot 3 + Vue 3 + UniApp + Spring AI** 的智能无人机租赁全栈平台，覆盖 **用户端 Web、跨端小程序、管理端 Web** 三端同源。完整业务闭环：**注册 → 资质审核 → 浏览 → 下单 → 支付 → 发货 → 收货 → 归还 → 评价 → 报修**。

> 🎓 应届毕业设计项目 · 13 张业务表 · 60+ REST 接口 · 15 个 Uniapp 页面 · 105 项 E2E 测试 100% 通过

---

## ✨ 核心能力

| 能力 | 说明 |
|------|------|
| 🚀 **完整业务闭环** | 注册/资质审核 → 浏览 → 下单 → 支付 → 发货 → 收货 → 归还 → 评价 → 报修 → 维修 |
| 🤖 **AI 智能客服** | 三级回复：本地知识库（< 100ms） → 通义千问 LLM（~1.5s） → 兜底模板 |
| 🔔 **WebSocket 实时通知** | 订单 / 评论 / 故障 / 空域备案多端实时推送，HandshakeInterceptor 解决 token 丢失 |
| 🛠 **Spring AI Agent** | 工具调用：无人机推荐 / 订单查询 / 维修记录查询 |
| 📱 **三端同源** | Web 端 + Uniapp（H5/小程序/App）共用同一套后端 API |
| 📊 **管理后台** | 仪表盘 / 订单 / 设备 / 用户 / 资质 / 维保 / AI 对话审计全覆盖 |
| 🔐 **JWT 鉴权** | 角色分级（USER / ADMIN），管理员 + 用户双登录入口 |
| 💾 **Redis 缓存** | 首页统计 / 无人机列表 / 用户信息，多态类型序列化解决 IPage 反序列化 |
| 🪪 **资质审核** | 飞行证书上传 → 待审核 → 通过 / 拒绝，未通过禁止下单 |
| 💬 **空域备案** | 用户提交空域 → 管理员审核通过后可用于下单 |
| 🛠 **故障报修** | 用户上报 → 管理员审核 → 自动生成维修工单 → 开始 / 完成 |
| 💰 **支付 + 退款** | 模拟支付 / 微信 / 支付宝；管理员主动退款 / 取消自动退款 + 库存回滚 |

---

## 🛠 技术栈

### 后端
- **Spring Boot 3.2** · Spring AI · Spring WebSocket · Spring Cache
- **MyBatis Plus 3.5** + **SQLite**（开发） / MySQL 8（生产）
- **Redis 7** + Jackson 多态类型序列化
- **JWT**（jjwt）· Swagger 3 · Lombok

### 前端
- **Web 端**：Vue 3.4 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + SCSS
- **跨端**：UniApp (Vue 3 + UTS) — H5 / 微信小程序 / iOS / Android

### AI
- **Spring AI** · Agent + Tool Calling
- **通义千问 Dashscope**（OpenAI 兼容协议）
- **RAG 知识库**（4 份本地文档：无人机租赁规则 / 民航法规 / 空域申请 / 保险规则）

### 工具链
- **Maven 3.9** · Java 23 · Node 20
- **Vite Proxy**（前端开发代理）· Git · HBuilderX

---

## 📁 项目结构

```
ai-drone-rental/
├── backend/                        # Spring Boot 后端
│   ├── src/main/java/com/drone/rental/
│   │   ├── ai/                     # AI Agent + RAG + Tools
│   │   ├── common/                 # Result / Constants / Exception
│   │   ├── config/                 # WebSocket / Cache / CORS / Swagger ...
│   │   ├── controller/             # 30+ REST 控制器
│   │   ├── dto/  entity/  vo/      # 数据传输对象
│   │   ├── encryption/             # 请求加密（AES）
│   │   ├── mapper/                 # MyBatis-Plus Mapper
│   │   ├── security/               # JWT 工具 + 拦截器
│   │   ├── service/impl/           # 业务实现
│   │   └── websocket/              # WebSocket Handler
│   ├── src/main/resources/
│   │   ├── application.yml         # 主配置
│   │   ├── knowledge-base/         # RAG 知识库
│   │   └── sql/init.sqlite.sql     # SQLite 初始化
│   └── pom.xml
│
├── frontend-web/                   # Vue 3 管理端 + 用户端
│   ├── src/
│   │   ├── api/                    # 接口封装（admin / user / public）
│   │   ├── components/             # 通用 + 业务组件
│   │   ├── layouts/                # AdminLayout / UserLayout
│   │   ├── router/  stores/        # 路由 + Pinia
│   │   ├── views/
│   │   │   ├── auth/               # 登录 / 注册
│   │   │   ├── admin/              # 12 个管理端页面
│   │   │   └── user/               # 13 个用户端页面
│   │   └── utils/                  # 加密 / 工具
│   ├── vite.config.js
│   └── package.json
│
├── frontend-uniapp/                # Uniapp 跨端
│   ├── pages/                      # 15 个页面（订单 / 资质 / 报修 / 空域 ...）
│   ├── api/                        # 接口封装
│   ├── static/drones/              # 8 张无人机图（内置）
│   ├── stores/  utils/
│   └── manifest.json
│
├── docs/                           # 设计文档
│   ├── 技术方案.md
│   ├── API接口示意图.md
│   ├── 操作手册.md
│   └── 项目分析报告.md
│
├── uploads/                        # 上传文件（资质 / 头像 / 报修图）
└── README.md
```

---

## 🚀 快速开始

### 环境要求
- **JDK 23+** · **Maven 3.9+** · **Node 20+** · **Redis 7+**
- （可选）HBuilderX（运行 Uniapp）

### 1. 启动 Redis

```bash
redis-server --daemonize yes   # 后台启动
redis-cli ping                 # 验证返回 PONG
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run            # → http://localhost:8080/api
# Swagger 文档：http://localhost:8080/api/swagger-ui/index.html
```

> ✅ 内置 SQLite，首次启动自动建表并写入示例数据（4 用户 / 8 无人机 / 11 订单）

### 3. 启动 Web 端

```bash
cd frontend-web
npm install
npm run dev                    # → http://localhost:5173
```

### 4. 启动 Uniapp 端（可选）

```bash
# 用 HBuilderX 打开 frontend-uniapp
# 运行 → 运行到浏览器 → Chrome（H5 模式）
# 或运行到微信小程序 / Android / iOS
```

### 🔑 默认账号

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | `admin` | `123456` | 全权限，含审核 / 仪表盘 / 维保 |
| 已审核用户 | `testuser` | `123456` | 已通过资质审核，可直接下单 |
| 待审核用户 | `newuser` | `123456` | 资质待审核，**下单会被拒** |

---

## 📡 API 模块

| 模块 | 路径前缀 | 鉴权 | 说明 |
|------|---------|------|------|
| 认证 | `/auth` | ❌ | 登录 / 注册 / 管理员登录 |
| 用户 | `/user` | ✅ | 资料 / 资质 / 充值 / 我的订单 |
| 无人机 | `/drone` | ❌ | 列表 / 详情 / 品牌 / 类型 / 评价 |
| 订单 | `/order` | ✅ | 创建 / 支付 / 收货 / 退租 / 退款 |
| 评价 | `/comment` | ❌/✅ | 公开评价查询 / 用户发表 |
| 空域备案 | `/airspace` | ✅ | 提交 / 我的列表 / 已审核列表 |
| 故障报修 | `/fault` | ✅ | 提交 / 我的 / 维修进度 |
| 通知 | `/notification` | ✅ | 用户通知 / 管理员通知 / 未读数 |
| AI 客服 | `/ai/v2` | ❌/✅ | 对话 / 知识库 / 状态（公开 + 上下文） |
| 文件上传 | `/common/upload` | ✅ | 头像 / 资质 / 报修图 |
| 管理端 | `/admin/*` | ✅ admin | 仪表盘 / 订单 / 设备 / 用户 / 审计 |
| 公开 | `/public/stats` | ❌ | 首页统计（免登录） |
| 调试 | `/ws/debug` | ✅ admin | WebSocket 在线管理员数 |

**完整接口文档**：[http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)

---

## 🔔 WebSocket 实时通知

```
ws://localhost:8080/api/ws/orders?token=<JWT>
```

| 事件 | type | 触发场景 |
|------|------|---------|
| 握手成功 | `connected` | WS 连接建立 |
| 心跳 | `pong` | 收到 `ping` 后回复 |
| 新订单 | `new_order` | 用户创建订单 |
| 新评论 | `new_comment` | 用户发表评价 |
| 新故障 | `new_fault` | 用户提交报修 |
| 新空域 | `new_airspace` | 用户提交空域备案 |

> 🛠 **Spring WebSocket 经典坑**：`session.getUri()` 在握手后丢失 query string，token 永远为 null。  
> ✅ **解决方案**：[HandshakeInterceptor](backend/src/main/java/com/drone/rental/config/WebSocketConfig.java) 在 HTTP 升级阶段把 token 塞进 `session.getAttributes()`。

---

## 🤖 AI 智能客服（三级回复）

```
用户问题
   ↓
1️⃣ 本地 RAG 知识库（< 100ms）→ 命中直接返回 + 文档出处
   ↓ 未命中
2️⃣ Spring AI Agent + 通义千问 LLM（~1.5s）→ 自然语言回复 + 工具调用
   ↓ 失败
3️⃣ 兜底模板 → 引导用户重试 / 留联系方式
   ↓
自动保存到 ai_chat_message 表，供管理端审计
```

**Agent 工具**：
- `queryAvailableDrones` — 按预算推荐无人机
- `queryUserOrders` — 查询用户订单
- `queryMaintenanceRecords` — 查询维修记录

**知识库**（[backend/src/main/resources/knowledge-base/](backend/src/main/resources/knowledge-base/)）：
- 无人机租赁规则.txt
- 民航法规.txt
- 空域申请流程.txt
- 无人机保险规则.txt

---

## 📊 订单状态机

```
待支付(0) ─→ 待发货(1) ─→ 待收货(2) ─→ 租赁中(3) ─→ 已归还(4)
   │            │             │
   ↓            ↓             ↓
已取消(5)    已退款(6)     已取消(5)
   ↑
自动退款 + 库存回滚
```

**业务规则**：
- ✅ 资质未通过 → **禁止下单**（拦截在 `OrderServiceImpl.createOrder`）
- ✅ 取消已支付订单 → **自动退款 + 库存回滚**
- ✅ 管理员可对 `已支付` 订单**主动退款**
- ✅ 故障审核通过 → **自动生成维修工单**

---

## 🖼 系统截图

> 仓库 `uploads/` 包含 8 张示例无人机图、3 张证书样例、用户上传样例。

| 模块 | 入口 |
|------|------|
| 用户端首页 | `http://localhost:5173/` |
| 用户登录 | `http://localhost:5173/login` |
| 用户注册 | `http://localhost:5173/register` |
| 无人机列表 | `http://localhost:5173/drones` |
| 管理端登录 | `http://localhost:5173/admin/login` |
| 管理端仪表盘 | `http://localhost:5173/admin/dashboard` |
| Swagger 文档 | `http://localhost:8080/api/swagger-ui/index.html` |

---

## 🗺 路线图

- [x] v1.0 — 基础租赁（用户 / 订单 / 设备）
- [x] v2.0 — 状态机 + 评价 + 资质审核
- [x] v3.0 — AI 客服 + 空域备案 + 故障报修 + WebSocket
- [x] v3.1 — Spring AI Agent + Tool Calling + RAG 知识库
- [x] v3.2 — 多态类型缓存 + HandshakeInterceptor 修复
- [ ] v4.0 — 真实微信 / 支付宝支付 + 地图定位 + 信用评分

---

## 🧪 质量保障

- ✅ **105/105** E2E 测试通过（注册 / 登录 / 资质 / 订单 / 评价 / 报修 / 维修 / 空域 / 通知 / WS / 缓存 / 三端）
- ✅ 关键路径：WebSocket 0 重连 · Redis 缓存命中毫秒级 · IPage 反序列化无 500
- ✅ 跨端：Uniapp 与 Web 共用同一套后端 API

---

## 📄 License

[MIT](LICENSE) — 自由使用、修改、分发，请保留版权声明。

---

<div align="center">

**如果觉得有帮助，请给一个 ⭐ Star！**

Made with ❤️ by [SpringAI1](https://github.com/SpringAI1)

</div>
