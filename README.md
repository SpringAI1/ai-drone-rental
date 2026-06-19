# 🚁 AI 无人机租赁系统

> **AI-Powered Drone Rental Platform** — 基于 **Spring Boot 3 + Vue 3 + Spring AI** 构建的智能无人机租赁平台，提供完整的租赁业务管理、AI 智能客服咨询和后台管理功能。

---

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green?style=for-the-badge&logo=spring)
![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D?style=for-the-badge&logo=vuedotjs)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)
![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=oracle)
![Swagger](https://img.shields.io/badge/Swagger-3.0-85EA2D?style=for-the-badge&logo=swagger)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

## 🌏 / English Version

Looking for the English version? Click here:
- [English README](./docs/README.en.md)  *(coming soon)*

---

## 📋 项目概述

本项目是一个现代化、智能化的无人机租赁管理平台，深度集成了人工智能技术，为用户提供 7×24 小时智能咨询服务。系统包含：

- 🧑‍💼 **用户端 Web**（Vue 3 + Element Plus）
- 📱 **用户端 UniApp 小程序**（支持 H5 / 微信小程序 / App）
- 👨‍💼 **管理员 Web**（Vue 3 + Element Plus）
- ⚙️ **后端 REST API**（Spring Boot 3 + MyBatis Plus）
- 🤖 **AI 智能客服**（Spring AI + 本地知识库 + LLM 兜底）
- 🔔 **实时消息通知**（WebSocket）

---

## 🎯 功能特性

### 🧑‍💼 用户端功能

| 模块 | 功能 | 说明 |
|------|------|------|
| **首页** | 热门设备 + 统计数据 | 展示热门无人机设备与平台统计，数据从后端实时获取 |
| **无人机列表** | 设备筛选与搜索 | 按品牌、价格、类型筛选，支持分页和高级搜索 |
| **设备详情** | 详细信息查看 | 查看设备参数、图片、评价，支持二级评论和回复 |
| **下单租赁** | 创建订单 | 选择租赁时长，生成订单，支持多种支付方式 |
| **订单管理** | 订单状态追踪 | 查看订单列表、订单详情、支付状态，订单状态独立页面展示 |
| **订单支付** | 模拟支付 | 支持微信、支付宝、账户余额三种支付方式 |
| **取消订单** | 取消待支付订单 | 支持输入取消原因，调用后端接口 |
| **申请退款** | 申请退款 | 待发货、待收货订单可申请退款 |
| **空域备案** | 空域备案申请 | 提交空域备案信息，等待审核 |
| **故障报修** | 设备故障上报 | 提交故障报告，上传图片 |
| **资质认证** | 用户资质审核 | 提交个人资质证明，等待审核 |
| **个人中心** | 用户信息管理 | 修改个人信息，查看信用记录 |
| **用户余额** | 余额充值 | 支持账户余额充值和支付 |
| **消息通知** | 消息推送 | 查看通知列表、未读数量、标记已读，支持实时推送 |
| **AI 助手** | 智能咨询服务 | 24/7 智能客服，支持无人机推荐、订单查询、故障咨询等 |

### 👨‍💼 管理端功能

| 模块 | 功能 | 说明 |
|------|------|------|
| **仪表盘** | 数据统计 | 订单统计、设备使用率、热门设备排行、AI 对话统计 |
| **用户管理** | 用户信息管理 | 用户列表、权限控制、信用管理 |
| **资质审核** | 审核用户资质 | 审核用户提交的资质证明 |
| **无人机管理** | 设备信息维护 | 添加、编辑、删除无人机设备，管理图片和参数 |
| **订单管理** | 订单审核处理 | 审核订单、确认发货、处理退款、处理归还 |
| **故障审核** | 故障报告审核 | 审核用户提交的故障报告，生成维保工单 |
| **空域备案审核** | 审核空域备案 | 审核用户提交的空域备案申请 |
| **维保管理** | 设备维护管理 | 维护工单创建、分配、完成、查看历史 |
| **评价管理** | 评价审核管理 | 查看、审核、回复用户评价 |
| **通知管理** | 系统通知管理 | 按类型筛选通知、标记已读、批量处理 |
| **AI 对话审计** | 查看用户对话 | 查看用户与 AI 的对话记录，用于优化本地知识库 |
| **AI 管理** | AI 助手配置 | AI 状态管理、维护消息设置、启用/禁用 AI |

---

## ✨ 版本特性

### v3.0（最新）

- 🤖 **AI 智能客服**：采用 Spring AI + Agent + Tool Calling 架构，支持无人机推荐、订单查询、维修记录查询等
- 📊 **订单状态分类页面**：独立的待支付、待发货、待收货、租赁中、已归还、已取消订单页面
- 🌍 **空域备案系统**：用户端提交申请 + 管理端审核
- 🔔 **消息通知系统**：实时推送 + 本地持久化 + 未读统计
- 📱 **UniApp 小程序支持**：支持 H5 / 微信小程序 / App 三端
- 🔧 **故障报修系统**：用户端提交 + 管理端审核 + 自动生成维保工单
- 📈 **公开统计接口**：首页统计数据无需认证，支持搜索引擎友好展示
- 👁️ **AI 对话审计**：管理端可查看所有用户与 AI 的对话记录，用于持续优化本地知识库

### v2.0

- ✅ 订单状态分类页面独立展示
- ✅ 二级评论和回复功能
- ✅ 响应式布局，适配移动端
- ✅ 面包屑导航
- ✅ 评论显示用户头像和昵称

### v1.0

- ✅ 基础租赁流程
- ✅ 登录/注册/用户中心
- ✅ 订单创建/支付/取消
- ✅ 设备浏览与详情

---

## 🛠 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.2.x | 后端框架 |
| **MyBatis Plus** | 3.5.x | ORM 框架 |
| **MySQL** | 8.x | 关系型数据库 |
| **Redis** | 7.x | 缓存系统（可选） |
| **Spring AI** | 1.0.0-M2 | AI 集成框架 |
| **阿里云 Dashscope** | - | AI 大模型（通义千问） |
| **JWT** | - | 身份认证 |
| **Swagger / SpringDoc** | 2.x | API 文档 |
| **WebSocket** | - | 实时消息推送 |
| **Druid** | 1.2.x | 数据库连接池 |

### 前端技术（管理端 Web）

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue** | 3.x | 前端框架 |
| **Vite** | 5.x | 构建工具 |
| **Element Plus** | 2.x | UI 组件库 |
| **Pinia** | 2.x | 状态管理 |
| **Vue Router** | 4.x | 路由管理 |
| **Axios** | 1.x | HTTP 请求 |
| **SCSS** | - | CSS 预处理器 |

### 前端技术（用户端 UniApp）

| 技术 | 版本 | 用途 |
|------|------|------|
| **uni-app** | 最新 | 跨端开发框架 |
| **Vue** | 3.x | 前端框架 |
| **uts** | - | 类型脚本 |
| **HBuilderX** | 最新 | IDE |

---

## 📁 项目结构

```
ai-drone-rental/
├── README.md                    # 项目说明文档（本文件）
├── .gitignore                   # Git 忽略配置
│
├── backend/                     # 后端项目（Spring Boot 3）
│   ├── pom.xml                  # Maven 依赖配置
│   ├── drone_rental.db          # SQLite 数据库（可选）
│   ├── sql/                     # 数据库脚本
│   │   ├── init.sql             # 完整初始化脚本
│   │   ├── ai_chat_message.sql  # AI 聊天记录表
│   │   ├── update_*.sql         # 增量更新脚本
│   │   └── ...
│   ├── uploads/                 # 图片上传目录（含示例图片）
│   │   ├── mavic3pro_drone.png
│   │   ├── mini4pro_drone.png
│   │   ├── air2s_drone.png
│   │   ├── agrast40_drone.png
│   │   ├── cert_*.png          # 资质示例图片
│   │   └── ...
│   └── src/main/java/com/drone/rental/
│       ├── DroneRentalApplication.java   # 主入口
│       ├── controller/          # REST API 控制器（共 20+）
│       │   ├── AuthController.java
│       │   ├── UserController.java
│       │   ├── OrderController.java
│       │   ├── DroneController.java
│       │   ├── CommentController.java
│       │   ├── FaultController.java
│       │   ├── NotificationController.java
│       │   ├── AirspaceController.java
│       │   ├── AdminAiChatController.java    # AI 对话审计
│       │   ├── AdminAirspaceController.java
│       │   ├── AdminCommentController.java
│       │   ├── AdminDashboardController.java
│       │   ├── AdminDroneController.java
│       │   ├── AdminMaintenanceController.java
│       │   ├── AdminOrderController.java
│       │   ├── AdminUserController.java
│       │   ├── AiController.java
│       │   ├── AiControllerV2.java          # 新版 AI 对话
│       │   ├── CommonController.java
│       │   ├── PublicStatsController.java
│       │   └── WebSocketDebugController.java
│       ├── service/             # 业务逻辑层
│       ├── service/impl/        # 服务实现
│       ├── mapper/              # MyBatis 数据访问层
│       ├── entity/              # 数据库实体（13 张表）
│       ├── dto/                 # 数据传输对象
│       ├── vo/                  # 视图对象
│       ├── config/              # 配置类（Spring AI / WebSocket / Swagger / CORS）
│       ├── security/            # 安全相关（JWT / 拦截器）
│       ├── common/              # 公共模块（Result / Constants）
│       ├── websocket/           # WebSocket 消息处理
│       └── resources/
│           ├── application.yml  # 应用配置
│           └── knowledge-base/  # AI 本地知识库（RAG 资料）
│               ├── drone_rental_rules.txt
│               ├── civil_aviation_regulations.txt
│               ├── airspace_application_process.txt
│               └── drone_insurance_regulations.txt
│
├── frontend-web/                # 管理端 Web（Vue 3 + Element Plus）
│   ├── index.html
│   ├── vite.config.js
│   ├── package.json
│   ├── package-lock.json
│   ├── stats.html
│   └── src/
│       ├── main.js              # 入口
│       ├── App.vue
│       ├── api/                 # API 请求封装（11 个模块）
│       │   ├── admin.js
│       │   ├── ai.js
│       │   ├── airspace.js
│       │   ├── auth.js
│       │   ├── comment.js
│       │   ├── drone.js
│       │   ├── maintenance.js
│       │   ├── notification.js
│       │   ├── order.js
│       │   ├── public.js
│       │   ├── request.js       # Axios 实例与拦截器
│       │   └── user.js
│       ├── components/          # 公共组件
│       │   ├── business/
│       │   │   └── DroneCard.vue
│       │   └── common/
│       │       ├── AiChat.vue       # AI 聊天组件
│       │       ├── BentoGrid.vue
│       │       ├── EmptyState.vue
│       │       ├── GlassCard.vue
│       │       ├── NotificationBell.vue  # 铃铛通知
│       │       ├── PageHeader.vue
│       │       ├── StatTile.vue
│       │       └── TimelineProgress.vue
│       ├── layouts/
│       │   ├── AdminLayout.vue
│       │   └── UserLayout.vue
│       ├── router/              # 路由配置
│       ├── stores/              # Pinia 状态管理
│       └── views/               # 页面视图（20+ 页面）
│           ├── NotFound.vue
│           ├── auth/            # 登录/注册
│           ├── admin/           # 管理端页面（13 个）
│           │   ├── Dashboard.vue
│           │   ├── UserManagement.vue
│           │   ├── AuditManagement.vue
│           │   ├── DroneManagement.vue
│           │   ├── OrderManagement.vue
│           │   ├── FaultAudit.vue
│           │   ├── AirspaceAudit.vue
│           │   ├── MaintenanceManagement.vue
│           │   ├── CommentManagement.vue
│           │   ├── NotificationManagement.vue
│           │   ├── AiManagement.vue
│           │   └── AiChatAudit.vue          # 新增：AI 对话审计
│           └── user/            # 用户端页面（14 个）
│               ├── Home.vue
│               ├── DroneList.vue
│               ├── DroneDetail.vue
│               ├── MyOrders.vue
│               ├── OrderPendingPay.vue
│               ├── OrderPendingShip.vue
│               ├── OrderPendingReceive.vue
│               ├── OrderRenting.vue
│               ├── OrderReturned.vue
│               ├── OrderCanceled.vue
│               ├── OrderDetail.vue
│               ├── OrderPay.vue
│               ├── Profile.vue
│               ├── AirspaceRecord.vue
│               ├── FaultReport.vue
│               └── Chat.vue
│
├── frontend-uniapp/            # 用户端 UniApp（跨端）
│   ├── manifest.json           # 应用配置
│   ├── pages.json              # 页面路由配置
│   ├── App.uvue               # 根组件
│   ├── main.uts               # 入口脚本
│   ├── uni.scss               # 全局样式
│   ├── index.html
│   ├── platformConfig.json
│   ├── api/                   # API 请求封装（ts）
│   │   ├── auth.ts
│   │   ├── ai.ts
│   │   ├── airspace.ts
│   │   ├── comment.ts
│   │   ├── drone.ts
│   │   ├── fault.ts
│   │   ├── notification.ts
│   │   ├── order.ts
│   │   ├── public.ts
│   │   └── user.ts
│   ├── pages/                 # 页面视图（10+）
│   │   ├── index/index.uvue
│   │   ├── chat/index.uvue
│   │   ├── drones/list.uvue
│   │   ├── drone-detail/index.uvue
│   │   ├── orders/list.uvue
│   │   ├── orders/detail.uvue
│   │   ├── orders/pay.uvue
│   │   ├── orders/comment.uvue
│   │   ├── airspace/record.uvue
│   │   ├── fault/report.uvue
│   │   ├── qualification/index.uvue
│   │   └── user/
│   │       ├── login.uvue
│   │       ├── register.uvue
│   │       ├── profile.uvue
│   │       ├── profile-edit.uvue
│   │       ├── change-password.uvue
│   │       ├── notifications.uvue
│   │       └── ...
│   ├── static/                # 静态资源（图片 / 图标）
│   │   ├── logo.png
│   │   ├── icons/             # 图标（40+ 张 SVG/PNG）
│   │   │   ├── home.svg
│   │   │   ├── order.svg
│   │   │   ├── user.svg
│   │   │   ├── drone.svg
│   │   │   ├── notification.png
│   │   │   ├── menu-*.png
│   │   │   └── stat-*.png
│   │   └── drones/            # 无人机示例图片
│   │       ├── mavic3pro_drone.png
│   │       ├── mini4pro_drone.png
│   │       ├── mini3pro_drone.png
│   │       ├── air2s_drone.png
│   │       ├── mavic3_drone.png
│   │       ├── phantom4pro_drone.png
│   │       ├── inspire2_drone.png
│   │       └── agrast40_drone.png
│   ├── stores/                # Pinia 状态管理（uts）
│   │   └── auth.ts
│   ├── utils/                 # 工具函数（ts）
│   │   ├── request.ts
│   │   ├── mock.ts
│   │   └── image.ts
│   ├── scripts/               # 辅助脚本
│   │   ├── generate-icons.js
│   │   └── generate-svg-icons.js
│   ├── unpackage/             # 编译产物（git 忽略）
│   └── ws-test2.py            # WebSocket 测试脚本
│
└── docs/                       # 项目文档
    ├── 技术方案.md             # 技术方案设计
    ├── 操作手册.md             # 用户操作手册
    ├── 项目分析报告.md         # 项目分析报告
    ├── UniApp微信小程序开发文档.md
    └── API接口示意图.md
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 推荐版本 | 用途 |
|------|---------|------|
| **Java** | 21+ | 后端运行环境 |
| **Node.js** | 18+ | 前端运行环境 |
| **MySQL** | 8.x | 数据库 |
| **Redis** | 7.x | 缓存系统（可选） |
| **Maven** | 3.9+ | 后端构建工具 |
| **HBuilderX** | 最新 | UniApp 开发 IDE（可选） |

### 1️⃣ 数据库配置

1. 创建数据库：

```sql
CREATE DATABASE drone_rental CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入初始化脚本：

```bash
mysql -u root -p drone_rental < backend/sql/init.sql
```

3. 更新数据库连接配置：

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/drone_rental?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
```

### 2️⃣ 后端启动

```bash
cd backend

# 编译项目
mvn clean compile

# 运行项目（开发模式）
mvn spring-boot:run
```

后端服务将在 **http://localhost:8080/api** 启动。

**Swagger API 文档**：`http://localhost:8080/api/swagger-ui/index.html`

### 3️⃣ 管理端 Web 启动

```bash
cd frontend-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 **http://localhost:3000**（或 vite 指定的端口）启动。

### 4️⃣ UniApp 小程序启动

1. 用 **HBuilderX** 打开 `frontend-uniapp/` 目录
2. 选择「运行」→「运行到浏览器」→「Chorme」进行 H5 预览
3. 或选择「运行」→「运行到小程序模拟器」→「微信开发者工具」

**注意**：如果没有 HBuilderX，也可以用命令行：

```bash
cd frontend-uniapp
npm install
# 根据 uni-app 项目配置的脚本运行
```

---

## 🔑 默认账号

| 角色 | 账号 | 密码 | 权限 |
|------|------|------|------|
| **普通用户** | `user` | `user123` | 浏览设备、下单租赁、空域备案、故障报修 |
| **管理员** | `admin` | `123456` | 全平台管理功能 |

> **安全提示**：密码已通过 MD5 加密存储在数据库中。生产环境请务必修改默认密码！

---

## 📡 API 接口总览

> 完整 API 文档请启动后端后访问：`http://localhost:8080/api/swagger-ui/index.html`

### 🔐 认证接口

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户登录 | POST | `/api/auth/login` | 用户端登录 |
| 用户注册 | POST | `/api/auth/register` | 用户端注册 |
| 管理员登录 | POST | `/api/auth/admin/login` | 管理端登录 |

### 👤 用户接口

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户信息 | GET | `/api/user/info` | 获取当前用户信息 |
| 更新信息 | PUT | `/api/user/info` | 更新用户信息 |
| 修改密码 | PUT | `/api/user/password` | 修改密码 |
| 订单列表 | GET | `/api/user/orders` | 获取用户订单列表 |
| 订单统计 | GET | `/api/user/order-stats` | 各状态订单数量 |
| 充值 | POST | `/api/user/recharge` | 账户余额充值 |
| 资质信息 | GET | `/api/user/qualification` | 获取飞行资质 |
| 提交资质 | POST | `/api/user/qualification` | 提交飞行资质 |

### 🛒 订单接口

| 功能 | 方法 | 路径 |
|------|------|------|
| 创建订单 | POST | `/api/order/create` |
| 订单详情 | GET | `/api/order/{id}` |
| 支付订单 | POST | `/api/order/{id}/pay` |
| 取消订单 | POST | `/api/order/{id}/cancel` |
| 确认收货 | POST | `/api/order/{id}/receive` |
| 申请退租 | POST | `/api/order/{id}/return` |
| 申请退款 | POST | `/api/order/{id}/refund` |
| 评论订单 | POST | `/api/order/{id}/comment` |
| 管理端订单列表 | GET | `/api/admin/orders` |
| 管理端审核 | POST | `/api/admin/order/{id}/review` |
| 管理端发货 | POST | `/api/admin/order/{id}/ship` |
| 管理端确认退款 | POST | `/api/admin/order/{id}/confirm-refund` |

### 📦 设备接口

| 功能 | 方法 | 路径 |
|------|------|------|
| 设备列表 | GET | `/api/drone/list` |
| 设备详情 | GET | `/api/drone/detail/{id}` |
| 品牌列表 | GET | `/api/drone/brands` |
| 类型列表 | GET | `/api/drone/types` |
| 设备评论 | GET | `/api/drone/{id}/comments` |

### 🌍 空域备案接口

| 功能 | 方法 | 路径 |
|------|------|------|
| 提交备案 | POST | `/api/airspace/submit` |
| 备案列表 | GET | `/api/airspace/list` |
| 备案详情 | GET | `/api/airspace/{id}` |
| 管理员列表 | GET | `/api/admin/airspace/list` |
| 审核备案 | PUT | `/api/admin/airspace/{id}/audit` |

### 🔧 故障报修接口

| 功能 | 方法 | 路径 |
|------|------|------|
| 提交报修 | POST | `/api/fault/submit` |
| 我的报修 | GET | `/api/fault/list` |
| 管理员列表 | GET | `/api/admin/fault/list` |
| 审核处理 | PUT | `/api/admin/fault/{id}/audit` |

### 🔔 消息通知接口

| 功能 | 方法 | 路径 |
|------|------|------|
| 通知列表 | GET | `/api/notification/list` |
| 未读数量 | GET | `/api/notification/unread-count` |
| 标记已读 | PUT | `/api/notification/{id}/read` |
| 全部已读 | PUT | `/api/notification/read-all` |
| 管理端列表 | GET | `/api/admin/notification/list` |
| **WebSocket** | ws:// | `/api/ws/notification` 实时推送 |

### 🤖 AI 智能客服接口

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AI 聊天 | POST | `/api/ai/chat` | 基础聊天 |
| **AI 聊天 V2** | POST | `/api/ai/v2/chat` | **本地知识库 + LLM** |
| AI 代理 | POST | `/api/ai/agent` | Agent 模式 |
| AI 状态 | GET | `/api/ai/status` | 获取 AI 状态 |
| AI 健康检查 | GET | `/api/ai/health` | 健康检查 |
| **搜索知识库** | POST | `/api/ai/v2/knowledge/search` | 搜索本地知识库 |
| **管理员设置 AI** | PUT | `/api/admin/ai/status` | 启用/禁用 AI |

### 🤖 AI 对话审计接口（管理端）

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 统计信息 | GET | `/api/admin/ai-chat/stats` | 会话数/消息数/活跃用户 |
| 会话列表 | GET | `/api/admin/ai-chat/conversations` | 分页查看用户会话 |
| 会话详情 | GET | `/api/admin/ai-chat/conversation/{id}` | 查看完整对话 |
| 删除会话 | DELETE | `/api/admin/ai-chat/conversation/{id}` | 删除历史对话 |

### 📊 公开接口

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 首页统计 | GET | `/api/public/stats` | **无需认证**，首页数据 |

---

## 🤖 AI 智能客服实现详解

本系统的 AI 助手采用 **三级回复机制**，通过本地知识库快速响应、大语言模型深度理解、智能工具调用业务系统，提供高质量对话体验。

### 三级回复机制

```
┌─────────────────────────────────────────────────────────────┐
│                    前端输入（用户问题）                        │
│          AiChat.vue（Vue） / chat/index.uvue（UniApp）        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌────────────────────────────────────────────────────────┐
    │ 1️⃣ 本地知识库（Local Knowledge）                       │
    │    优先级最高，响应 <100ms                              │
    │    覆盖：价格/流程/押金/设备/物流/空域等常见问题         │
    │    基于关键字匹配 + 模板回复                           │
    └────────────────────────────────────────────────────────┘
                              │          ┌────── 命中 ───────┐
                              ▼          ▼                   │
    ┌───────────────────────────────────────────────────┐     │
    │ 2️⃣ Spring AI + 通义千问大模型（LLM）              │     │
    │    需要 API Key，响应 ~1.5s                        │     │
    │    基于对话上下文 + 系统 Prompt 生成自然语言回答    │     │
    │    通过阿里云 Dashscope API 调用                    │     │
    └───────────────────────────────────────────────────┘     │
                              │         ┌─────── 失败 ───────┐
                              ▼         ▼                    │
    ┌───────────────────────────────────────────────────┐     │
    │ 3️⃣ 兜底模板（Fallback Template）                 │     │
    │    提示用户拨打客服或重试                           │     │
    └───────────────────────────────────────────────────┘     │
                              │                                │
                              ▼                                ▼
                    ┌──────────────────────────┐
                    │      ✅ 返回回复           │
                    │  （自动保存到数据库审计）   │
                    └──────────────────────────┘
```

### AI 配置

在 `backend/src/main/resources/application.yml` 中配置：

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-dashscope-api-key-here
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-turbo
          temperature: 0.3
          max-tokens: 2000
```

> 💡 支持 OpenAI 兼容接口（如：通义千问 Dashscope、DeepSeek、Ollama 等）

### 本地知识库

知识库文件位于 `backend/src/main/resources/knowledge-base/`：

| 文件 | 内容 |
|------|------|
| `drone_rental_rules.txt` | 租赁规则与流程 |
| `civil_aviation_regulations.txt` | 民航相关法规 |
| `airspace_application_process.txt` | 空域备案流程 |
| `drone_insurance_regulations.txt` | 无人机保险说明 |

管理员可通过「AI 对话审计」功能查看用户实际提问，补充优化本地知识库。

---

## 📊 订单状态流转

```
创建订单 → 待支付(0) → 待发货(1) → 待收货(2) → 租赁中(3) → 已归还(4)
                     │              │              │
                     ▼              ▼              ▼
                 已取消(5)       已退款(6)      已退款(6)
```

| 状态码 | 名称 | 可执行操作 |
|--------|------|------------|
| 0 | 待支付 | 支付 / 取消订单 |
| 1 | 待发货 | 申请退款（用户）/ 发货（管理员） |
| 2 | 待收货 | 确认收货 / 申请退款 |
| 3 | 租赁中 | 申请退租 |
| 4 | 已归还 | 发表评价 |
| 5 | 已取消 | - |
| 6 | 已退款 | - |

---

## 🔔 WebSocket 实时消息

本系统集成了 WebSocket 消息推送，支持以下实时通知场景：

| 事件 | 触发方 | 通知对象 |
|------|--------|----------|
| 新订单创建 | 用户下单 | 管理员铃铛 +1 |
| 订单状态更新 | 管理员操作 | 用户端订单列表 |
| 故障报修提交 | 用户提交 | 管理员通知 |
| 空域备案提交 | 用户提交 | 管理员通知 |
| AI 对话新消息 | 系统触发 | 管理端审计 |

前端接入示例（`utils/request.ts` 或组件内部）：

```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/notification')
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  console.log('新消息:', data)
  // unreadCount++ + Toast 提示
}
```

---

## 📱 移动端 / 小程序支持

本系统支持 UniApp 跨端开发，共用同一套后端 API。详细开发文档：

- [UniApp微信小程序开发文档](./docs/UniApp微信小程序开发文档.md)
- [API接口示意图](./docs/API接口示意图.md)

| 平台 | 技术栈 | 推荐组件库 |
|------|--------|------------|
| H5 | Vue 3 + uni-app | uni-ui / Element Plus |
| 微信小程序 | 原生 / uni-app | Vant Weapp / WeUI |
| iOS/Android App | uni-app 编译 | uView / uni-ui |

---

## 📝 开发规范

### 代码风格

- ✅ 使用 TypeScript（前端）/ Java 21（后端）
- ✅ 函数组件 + Hooks 风格，不用 class 组件
- ✅ 使用 `const` 而非 `let`，除非需要重新赋值
- ✅ 使用箭头函数，避免 `function` 关键字
- ✅ 使用模板字符串，不用字符串拼接

### Git 提交规范

```
feat:     新功能
fix:      修复 bug
docs:     文档
style:    格式（不影响代码运行）
refactor: 重构
test:     测试
chore:    构建/工具
```

### 目录命名规范

```
backend/       Spring Boot 后端
frontend-web/  Vue 3 管理端 Web
frontend-uniapp/  UniApp 用户端
docs/          项目文档（Markdown）
```

---

## 📸 项目截图预览

无人机设备图片资源位于：

- `backend/uploads/` — 后端上传示例（含无人机图片和资质图片）
- `frontend-uniapp/static/drones/` — 小程序端无人机示例图片
- `frontend-uniapp/static/icons/` — 小程序端图标（SVG/PNG，40+）

主要设备型号：
- 🏆 DJI Mavic 3 Pro（旗舰机型）
- 🚁 DJI Air 2S（专业航拍）
- 🛩 DJI Mini 4 Pro（入门首选）
- 🌾 DJI Agras T40（农业植保）

---

## 🔧 常见问题

**Q1：启动后端时提示数据库连接失败？**

请确认：1) MySQL 服务已启动；2) `drone_rental` 数据库已创建；3) `application.yml` 中数据库账号密码正确。

**Q2：AI 智能客服不回复？**

检查：1) API Key 是否正确；2) 网络是否可达通义千问 API；3) 若使用完全本地模式，可在管理端「AI 管理」中关闭 LLM，使用纯本地知识库。

**Q3：前端页面请求 API 失败？**

检查 `utils/request.ts` 的 baseURL 是否指向你的后端地址（默认 `http://localhost:8080/api`）。

**Q4：小程序真机调试提示跨域？**

请在微信开发者工具的「项目设置」中勾选「不校验合法域名」。

---

## 🗺 路线图

- [x] v1.0 — 基础租赁流程
- [x] v2.0 — 订单状态分类页面 + 二级评论
- [x] v3.0 — AI 智能客服 + 空域备案 + 故障报修 + 消息通知 + AI 对话审计
- [ ] v4.0（计划）— 支付宝/微信真实支付对接、地图无人机定位、信用评分模型
- [ ] v5.0（计划）— 推荐算法优化、多租户 SaaS 架构、国际化支持

---

## 📄 许可证

本项目采用 **MIT 许可证** — 你可以自由使用、修改、分发，但需保留版权声明。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！贡献代码前请：

1. Fork 本仓库
2. 创建你的特性分支（`git checkout -b feature/AmazingFeature`）
3. 提交你的改动（`git commit -m 'feat: add some amazing feature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 打开 Pull Request

---

## 📮 联系方式

- **项目地址**：[https://github.com/SpringAI1/ai-drone-rental](https://github.com/SpringAI1/ai-drone-rental)
- **客服电话**：400-800-8888
- **邮箱**：support@drone-rental.com

---

<div align="center">

**如果本项目对你有帮助，请给一个 ⭐ Star 支持！**

Made with ❤️ by SpringAI Team

</div>
