# 🚁 AI 无人机租赁系统

> **AI-Powered Drone Rental Platform** — 基于 **Spring Boot 3 + Vue 3 + UniApp + Spring AI** 构建的智能无人机租赁全栈平台，提供完整的租赁业务流程、AI 智能客服、实时消息通知与后台管理功能。

---

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-6DB33F?style=for-the-badge&logo=spring)
![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D?style=for-the-badge&logo=vuedotjs)
![uni-app](https://img.shields.io/badge/UniApp-H5%20%7C%20%E5%BE%AE%E4%BF%A1-22B14C?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5-222222?style=for-the-badge)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F?style=for-the-badge&logo=spring)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger)
![WebSocket](https://img.shields.io/badge/WebSocket-FF4D00?style=for-the-badge&logo=socketdotio)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

## 📋 项目概述

本项目是一个**现代化、智能化**的无人机租赁管理平台，深度集成了 Spring AI 人工智能技术，为用户提供 7×24 小时智能咨询服务。系统采用前后端分离架构，完整覆盖从用户浏览、下单、支付、租赁、归还给付与评价，到管理员审核、发货、维保、结算的全业务流程。

### 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                          客户端（三端同源）                            │
│  ┌────────────┐  ┌─────────────┐  ┌────────────────────┐              │
│  │ Web 管理端 │  │  Web 用户端 │  │ UniApp(H5/小程序/App) │           │
│  └─────┬──────┘  └──────┬──────┘  └──────────┬─────────┘              │
└────────┼────────────────┼────────────────────┼──────────────────────────┘
         │                │                    │
         ▼                ▼                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          后端 API 网关                                 │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │                Spring Boot 3 (端口: 8080, context: /api)     │    │
│  │  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌────┐    │    │
│  │  │  认证   │ │  用户    │ │  无人机  │ │   订单   │ │AI  │    │    │
│  │  │ JWT    │ │  资质    │ │  详情    │ │  支付    │ │对话│    │    │
│  │  └─────────┘ └──────────┘ └─────────┘ └──────────┘ └────┘    │    │
│  │  ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌────────────┐    │    │
│  │  │  空域备案  │ │ 故障报修   │ │  评价     │ │ 消息通知   │    │    │
│  │  └────────────┘ └────────────┘ └──────────┘ └────────────┘    │    │
│  └──────────────────────────────────────────────────────────────┘    │
└────────┬────────────────────────────────────────────────┬─────────────┘
         │                                                │
         ▼                                                ▼
┌──────────────────┐                          ┌──────────────────────┐
│  MySQL 数据库    │                          │  WebSocket 实时推送   │
│  (drone_rental)  │                          │  (订单/通知/审计)     │
└──────────────────┘                          └──────────────────────┘
```

---

## 🎯 功能特性

### 🧑‍💼 用户端（Web + UniApp）

| 模块 | 功能说明 |
|------|---------|
| **用户认证** | 注册、登录、退出、JWT 鉴权 |
| **个人中心** | 头像上传、资料修改、改密码、余额充值、信用记录 |
| **无人机浏览** | 列表、筛选（品牌/类型/价格区间）、排序、分页、详情页 |
| **设备参数** | 续航、载重、速度、航程、图片、评价展示 |
| **下单租赁** | 资质校验、库存校验、选择租期、填写收货地址、自动生成订单号 |
| **订单管理** | 订单列表（按状态分类展示）、订单详情、支付、取消、收货、归还、退款 |
| **订单支付** | 模拟余额/微信/支付宝支付；支付后状态自动流转 |
| **订单取消** | 支持待支付、待发货、待收货状态取消，已支付订单自动退款 |
| **用户评价** | 已完成订单可发表评价、支持图片上传、支持删除 |
| **飞行资质** | 上传资质证明、查看审核状态、资质未通过禁止下单 |
| **空域备案** | 提交飞行空域信息、经纬度、高度、时间、目的；查看审核状态 |
| **故障报修** | 对订单设备提交故障报告、上传图片、查看审核进度 |
| **消息通知** | 通知列表、未读数量、标记已读、全部已读（WebSocket 实时推送） |
| **AI 智能客服** | 24/7 智能问答、本地知识库快速响应、通义千问兜底、可搜索知识库 |

### 👨‍💼 管理端（Web）

| 模块 | 功能说明 |
|------|---------|
| **仪表盘** | 订单总览、设备使用率、用户统计、AI 对话统计 |
| **用户管理** | 用户列表、查看详情、权限控制、信用管理 |
| **资质审核** | 用户提交资质审核、通过/拒绝、审核备注 |
| **无人机管理** | 添加/编辑/删除设备、图片管理、上下架、库存调整、参数维护 |
| **订单管理** | 订单全列表、按状态筛选、发货、确认收货、退款处理 |
| **故障审核** | 审核用户提交故障、生成维保工单 |
| **维保管理** | 维保工单列表、状态流转、维护记录 |
| **空域审核** | 审核用户空域备案、备注说明 |
| **评价管理** | 查看/删除用户评价 |
| **通知管理** | 通知列表、批量已读处理 |
| **AI 对话审计** | 查看所有用户与 AI 的对话记录、会话统计、用于优化知识库 |
| **AI 管理** | 启用/禁用 AI 服务、配置欢迎语与默认提示 |

---

## ✨ 版本特性

### v3.0（当前）

- 🤖 **AI 智能客服**：Spring AI + Agent + Tool Calling 架构，支持无人机推荐、订单查询、维修查询
- 📊 **订单状态分类独立页面**：待支付、待发货、待收货、租赁中、已归还、已取消、已退款独立路由
- 🌍 **空域备案系统**：用户端提交 + 管理端审核，支持经纬度和飞行参数
- 🔔 **消息通知系统**：实时 WebSocket 推送 + 本地持久化 + 未读统计
- 📱 **UniApp 跨端支持**：H5 / 微信小程序 / App 三端同源，共用同一套后端 API
- 🔧 **故障报修系统**：用户端提交 → 管理端审核 → 自动生成维保工单
- 📈 **公开统计接口**：首页统计数据无需认证，SEO 友好
- 👁️ **AI 对话审计**：管理端查看所有用户与 AI 的对话，辅助知识库持续优化

### v2.0

- ✅ 订单状态分类独立页面
- ✅ 二级评论和回复功能
- ✅ 响应式布局，适配移动端
- ✅ 评论显示用户头像和昵称

### v1.0

- ✅ 基础租赁流程
- ✅ 登录/注册/用户中心
- ✅ 订单创建/支付/取消
- ✅ 设备浏览与详情

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.2.x | 后端框架 |
| **Spring AI** | 1.0.0-M2 | AI 集成框架 |
| **MyBatis Plus** | 3.5.x | ORM 框架 |
| **MySQL** | 8.x | 关系型数据库 |
| **Redis** | 7.x | 缓存（可选） |
| **JWT** | 0.11.5 | 身份认证 |
| **Druid** | 1.2.x | 数据库连接池 |
| **SpringDoc / Swagger** | 2.x | API 文档 |
| **WebSocket** | — | 实时消息推送 |
| **通义千问 Dashscope** | — | AI 大模型（兼容 OpenAI API） |

### 前端（Web 用户端 + 管理端）

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue** | 3.x | 前端框架 |
| **Vite** | 5.x | 构建工具 |
| **Element Plus** | 2.x | UI 组件库 |
| **Pinia** | 2.x | 状态管理 |
| **Vue Router** | 4.x | 路由管理 |
| **Axios** | 1.x | HTTP 请求库 |
| **SCSS** | — | CSS 预处理器 |

### 前端（UniApp 跨端）

| 技术 | 版本 | 用途 |
|------|------|------|
| **uni-app (Vue 3)** | 最新 | 跨端开发框架（H5 / 微信小程序 / App） |
| **UTS / TypeScript** | — | 类型脚本 |
| **HBuilderX** | 最新 | IDE（推荐） |

---

## 📁 项目结构

```
AI-Rental/
├── README.md                              # 项目说明（本文件）
├── .gitignore                             # Git 忽略
│
├── backend/                               # 后端（Spring Boot 3）
│   ├── pom.xml                            # Maven 依赖配置
│   ├── sql/                               # 数据库脚本
│   │   ├── init.sql                       # 完整初始化脚本
│   │   ├── ai_chat_message.sql            # AI 聊天记录表
│   │   ├── update_cert_images.sql         # 资质图片更新
│   │   ├── update_delivery_address.sql    # 地址字段更新
│   │   └── update_drone_images.sql        # 无人机图片更新
│   ├── uploads/                           # 图片上传目录（含示例图片）
│   │   ├── mavic3_drone.png              # 无人机图片
│   │   ├── mavic3pro_drone.png
│   │   ├── mini3pro_drone.png
│   │   ├── mini4pro_drone.png
│   │   ├── air2s_drone.png
│   │   ├── phantom4pro_drone.png
│   │   ├── inspire2_drone.png
│   │   ├── agrast40_drone.png
│   │   ├── cert_1.png ~ cert_3.png       # 资质示例图片
│   │   └── ...
│   └── src/main/
│       ├── java/com/drone/rental/
│       │   ├── DroneRentalApplication.java       # 启动类
│       │   ├── controller/                       # REST 控制器（20+）
│       │   │   ├── AuthController.java           # 认证
│       │   │   ├── UserController.java           # 用户
│       │   │   ├── OrderController.java          # 订单
│       │   │   ├── DroneController.java          # 无人机
│       │   │   ├── CommentController.java        # 评价
│       │   │   ├── AirspaceController.java       # 空域备案
│       │   │   ├── FaultController.java          # 故障报修
│       │   │   ├── NotificationController.java   # 消息通知
│       │   │   ├── AiControllerV2.java           # AI 对话（新版）
│       │   │   ├── AiController.java             # AI 对话（旧版）
│       │   │   ├── CommonController.java         # 文件上传
│       │   │   ├── PublicStatsController.java    # 公开统计
│       │   │   ├── AdminDashboardController.java # 管理仪表盘
│       │   │   ├── AdminOrderController.java     # 订单管理
│       │   │   ├── AdminDroneController.java     # 无人机管理
│       │   │   ├── AdminMaintenanceController.java # 维保管理
│       │   │   ├── AdminCommentController.java   # 评价管理
│       │   │   ├── AdminAirspaceController.java  # 空域审核
│       │   │   ├── AdminUserController.java      # 用户管理
│       │   │   ├── AdminAiChatController.java    # AI 对话审计
│       │   │   └── WebSocketDebugController.java # WS 调试
│       │   ├── service/               # 服务接口
│       │   ├── service/impl/          # 服务实现（14 个）
│       │   ├── mapper/                # MyBatis Mapper（13 个）
│       │   ├── entity/                # 数据库实体（13 张表）
│       │   ├── dto/                   # 请求 DTO
│       │   ├── vo/                    # 响应 VO
│       │   ├── config/                # 配置类
│       │   │   ├── SpringAiConfig.java
│       │   │   ├── WebMvcConfig.java
│       │   │   ├── WebSocketConfig.java
│       │   │   ├── SwaggerConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   └── ...
│       │   ├── security/              # JWT / 拦截器 / 用户上下文
│       │   ├── common/                # Result、ResultCode、Constants
│       │   ├── websocket/             # WebSocket 端点与处理器
│       │   └── ai/                    # AI 相关模块
│       │       ├── agent/             # DroneRentalAgent（Agent 模式）
│       │       ├── rag/               # RagService（本地知识库 RAG）
│       │       └── tools/             # AiTools、SpringAiTools
│       └── resources/
│           ├── application.yml        # 应用配置
│           └── knowledge-base/        # AI 本地知识库
│               ├── drone_rental_rules.txt
│               ├── civil_aviation_regulations.txt
│               ├── airspace_application_process.txt
│               └── drone_insurance_regulations.txt
│
├── frontend-web/                        # Web 端（Vue 3 + Element Plus）
│   ├── index.html
│   ├── vite.config.js                   # Vite 配置（端口 3000，代理 /api）
│   ├── package.json
│   ├── stats.html
│   └── src/
│       ├── main.js                      # 入口
│       ├── App.vue
│       ├── api/                         # API 请求模块（11 个）
│       │   ├── auth.js                  # 认证
│       │   ├── user.js                  # 用户/资质/充值
│       │   ├── drone.js                 # 无人机
│       │   ├── order.js                 # 订单
│       │   ├── comment.js               # 评价
│       │   ├── airspace.js              # 空域
│       │   ├── fault.js                 # 故障
│       │   ├── notification.js          # 通知
│       │   ├── ai.js                    # AI 对话
│       │   ├── admin.js                 # 管理端综合
│       │   ├── maintenance.js           # 维保
│       │   ├── public.js                # 公开接口
│       │   └── request.js               # Axios 实例 + 拦截器
│       ├── components/                  # 公共组件
│       │   ├── business/
│       │   │   └── DroneCard.vue        # 无人机卡片
│       │   └── common/
│       │       ├── AiChat.vue           # AI 聊天组件
│       │       ├── BentoGrid.vue
│       │       ├── EmptyState.vue
│       │       ├── GlassCard.vue
│       │       ├── NotificationBell.vue  # 消息铃铛
│       │       ├── PageHeader.vue
│       │       ├── StatTile.vue
│       │       ├── StatusTag.vue         # 状态标签（danger/success/...）
│       │       └── TimelineProgress.vue
│       ├── layouts/                     # 布局
│       │   ├── AdminLayout.vue
│       │   └── UserLayout.vue
│       ├── router/                      # Vue Router 路由
│       ├── stores/                      # Pinia 状态管理
│       └── views/                       # 页面视图（25+）
│           ├── NotFound.vue
│           ├── auth/                    # Login.vue / Register.vue
│           ├── admin/                   # 管理端 13 个页面
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
│           │   └── AiChatAudit.vue
│           └── user/                    # 用户端 14 个页面
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
├── frontend-uniapp/                    # UniApp 跨端（H5 / 小程序 / App）
│   ├── manifest.json                    # 应用配置
│   ├── pages.json                       # 页面路由
│   ├── App.uvue                         # 根组件
│   ├── main.uts                         # 入口脚本（UTS）
│   ├── uni.scss                         # 全局样式
│   ├── index.html
│   ├── platformConfig.json
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.js
│   ├── src/                             # H5 兼容源目录（Vue 3 + TS）
│   │   ├── App.vue
│   │   ├── main.ts
│   │   ├── api/                         # 与 Web 端同源 API（10 个模块）
│   │   ├── stores/auth.ts
│   │   ├── utils/
│   │   │   ├── request.ts               # uni.request 封装 + loading
│   │   │   ├── mock.ts                  # 本地 mock（网络失败时兜底）
│   │   │   └── image.ts                 # 图片 URL 处理（H5/小程序/App）
│   │   └── pages/                       # 页面（13+ 个）
│   │       ├── index/index.vue
│   │       ├── chat/index.vue
│   │       ├── drones/list.vue
│   │       ├── drone-detail/index.vue   # 含地址选择与手动填写
│   │       ├── orders/list.vue
│   │       ├── orders/detail.vue
│   │       ├── orders/pay.vue
│   │       ├── orders/comment.vue
│   │       ├── airspace/record.vue
│   │       ├── fault/report.vue
│   │       ├── qualification/index.vue
│   │       └── user/login.vue / register.vue / profile.vue / ...
│   ├── pages/                           # 小程序/App 专用页面（.uvue）
│   ├── static/                          # 静态资源
│   │   ├── logo.png
│   │   ├── drones/                      # 无人机示例图
│   │   └── icons/                       # 图标 SVG/PNG（40+）
│   ├── stores/auth.ts
│   ├── utils/request.ts / mock.ts / image.ts
│   └── scripts/                         # 辅助脚本（图标生成等）
│
└── docs/                                 # 项目文档
    ├── 技术方案.md
    ├── 操作手册.md
    ├── 项目分析报告.md
    ├── UniApp微信小程序开发文档.md
    └── API接口示意图.md
```

---

## 🚀 快速开始

### 环境要求

| 依赖 | 推荐版本 | 最低版本 | 用途 |
|------|---------|---------|------|
| **Java** | 21 | 17 | 后端运行环境 |
| **Node.js** | 20 | 18 | 前端运行环境 |
| **MySQL** | 8.0 | 5.7 | 关系型数据库 |
| **Maven** | 3.9 | 3.8 | 后端构建工具 |
| **npm / pnpm** | 10 | 9 | 前端包管理 |
| **HBuilderX** | 最新 | - | UniApp 开发 IDE（可选） |

---

### 1️⃣ 数据库配置

**创建数据库并导入数据：**

```sql
CREATE DATABASE drone_rental
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE drone_rental;
SOURCE /path/to/backend/sql/init.sql;
```

**更新数据库连接（`backend/src/main/resources/application.yml`）：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/drone_rental?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
```

---

### 2️⃣ 后端启动

后端端口 **8080**，Context Path **/api**。

```bash
cd backend

# 编译（首次建议）
mvn clean compile

# 开发模式运行
mvn spring-boot:run
```

✅ 启动成功后访问：
- 服务根路径：`http://localhost:8080/api`
- Swagger API 文档：`http://localhost:8080/api/swagger-ui/index.html`
- 静态图片：`http://localhost:8080/api/uploads/mavic3_drone.png`

---

### 3️⃣ Web 端启动

Web 端同时包含**管理端**和**用户端**，端口 **3000**，API 请求通过 Vite 代理 `/api` → `http://localhost:8080/api`。

```bash
cd frontend-web

# 安装依赖（首次）
npm install

# 启动开发服务器
npm run dev
```

✅ 启动成功后访问：`http://localhost:3000`

---

### 4️⃣ UniApp 跨端启动

#### 方式 A：HBuilderX 可视化运行（推荐）

1. 用 **HBuilderX** 打开 `frontend-uniapp/` 目录
2. 顶部菜单：「运行」→「运行到浏览器」→「Chrome」（H5 预览）
3. 或：「运行」→「运行到小程序模拟器」→「微信开发者工具」

#### 方式 B：命令行运行

```bash
cd frontend-uniapp
npm install
npm run dev:h5        # H5 模式
npm run dev:mp-weixin # 微信小程序
```

✅ 默认通过 Vite 启动 H5 开发服务器：`http://localhost:5173`

> 💡 UniApp 端的请求地址在 `src/utils/request.ts` 中统一配置：
> - H5 模式：走相对路径 `/api`（由 Vite 代理）
> - 小程序/App 模式：直连 `http://localhost:8080/api`（生产环境需修改为域名）

---

## 🔑 默认账号

数据库初始化脚本已预置以下测试账号：

| 角色 | 账号 | 密码 | 权限说明 |
|------|------|------|---------|
| 👨‍💼 管理员 | `admin` | `123456` | 全平台管理功能、AI 审计、订单/设备/用户管理 |
| 🧑‍💼 普通用户 | `lisi` | `123456` | 已通过资质审核，可正常下单、测试全流程 |
| 🧑‍💼 普通用户 | `zhangxi` | `123456` | 已通过资质审核，备用测试账号 |
| 🧑‍💼 普通用户 | `zhangsan` | `123456` | 普通账号 |

> ⚠️ **安全提示**：以上密码已加密存储，仅供本地开发/演示。生产环境请务必删除默认账号或修改为强密码。

---

## 📡 API 接口总览

> 🔍 **完整 API 文档**（含在线调试）：启动后端后访问
> `http://localhost:8080/api/swagger-ui/index.html`

### 认证接口 (`/auth`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录（获取 JWT） | ❌ |
| POST | `/auth/register` | 用户注册 | ❌ |
| POST | `/auth/admin/login` | 管理员登录 | ❌ |
| POST | `/auth/logout` | 退出登录 | ✅ |

### 用户接口 (`/user`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/user/info` | 获取当前用户信息 | ✅ |
| PUT | `/user/info` | 更新用户资料（昵称/手机/头像/地址） | ✅ |
| PUT | `/user/password` | 修改密码 | ✅ |
| POST | `/user/recharge` | 账户余额充值 | ✅ |
| GET | `/user/qualification` | 获取飞行资质 | ✅ |
| POST | `/user/qualification` | 提交飞行资质 | ✅ |
| GET | `/user/orders` | 我的订单列表（分页+状态筛选） | ✅ |
| GET | `/user/order-stats` | 各状态订单数量统计 | ✅ |

### 无人机接口 (`/drone`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/drone/list` | 无人机列表（筛选+分页+排序） | ❌ |
| GET | `/drone/detail/{id}` | 无人机详情 | ❌ |
| GET | `/drone/brands` | 所有品牌 | ❌ |
| GET | `/drone/types` | 所有类型 | ❌ |
| GET | `/drone/{id}/comments` | 某无人机的评价列表（分页） | ❌ |
| PUT | `/drone/{id}/image` | 更新无人机图片（管理端） | ✅ |

### 订单接口 (`/order`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/order/create` | 创建订单（需资质+库存校验） | ✅ |
| GET | `/order/{id}` | 订单详情 | ✅ |
| POST | `/order/{id}/pay` | 支付订单（余额/微信/支付宝） | ✅ |
| POST | `/order/{id}/cancel` | 取消订单（已支付自动退款） | ✅ |
| POST | `/order/{id}/receive` | 确认收货 | ✅ |
| POST | `/order/{id}/return` | 申请退租 | ✅ |
| POST | `/order/{id}/refund` | 申请退款 | ✅ |

### 评价接口 (`/comment`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/comment/add` | 发表评价（图片+评分+内容） | ✅ |
| GET | `/comment/my` | 我的评价（分页） | ✅ |
| DELETE | `/comment/{id}` | 删除我的评价 | ✅ |

### 空域备案 (`/airspace`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/airspace/submit` | 提交空域备案 | ✅ |
| GET | `/airspace/list` | 我的备案列表 | ✅ |
| GET | `/airspace/approved` | 我已通过的备案 | ✅ |

### 故障报修 (`/fault`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/fault/submit` | 提交故障报告 | ✅ |
| GET | `/fault/my` | 我的报修列表（分页） | ✅ |
| GET | `/fault/order/{orderId}` | 按订单查询报修 | ✅ |
| GET | `/fault/{id}` | 报修详情 | ✅ |

### 消息通知 (`/notification`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/notification/list` | 通知列表（分页） | ✅ |
| GET | `/notification/unread-count` | 未读数量 | ✅ |
| PUT | `/notification/{id}/read` | 标记单条已读 | ✅ |
| PUT | `/notification/read-all` | 全部标记已读 | ✅ |
| WS | `/ws/notification` | **WebSocket 实时推送** | ✅ |

### AI 智能客服 (`/ai` + `/ai/v2`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/ai/v2/chat` | **推荐** AI 对话（本地知识库 + LLM） | ✅ |
| POST | `/ai/v2/knowledge/search` | 搜索本地知识库 | ✅ |
| GET | `/ai/v2/status` | AI 服务状态 | ❌ |
| POST | `/ai/chat` | 基础 AI 聊天（兼容老版本） | ✅ |
| POST | `/ai/agent` | Agent 模式（工具调用） | ✅ |

### 文件上传 (`/common`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/common/upload` | 文件/图片上传（multipart/form-data） | ✅ |

### 公开接口 (`/public`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/public/stats` | 首页统计数据（订单数/无人机数/用户数/好评率） | ❌ |

### 管理端接口 (`/admin/*`)

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 仪表盘 | `/admin/dashboard` | 仪表盘汇总数据 |
| 订单管理 | `/admin/order` | 订单列表/详情/发货/退款/审核 |
| 无人机管理 | `/admin/drone` | 设备 CRUD / 库存 / 上下架 |
| 维保管理 | `/admin/maintenance` | 维保工单 CRUD / 状态流转 |
| 评价管理 | `/admin/comment` | 评价列表 / 删除 |
| 空域审核 | `/admin/airspace` | 空域备案审核 / 列表 |
| 用户管理 | `/admin/user` | 用户列表 / 详情 |
| AI 对话审计 | `/admin/ai-chat` | 会话统计/会话列表/会话详情/删除 |
| 通知管理 | `/admin/notification` | 管理员通知列表 / 未读数量 / 标记已读 |

---

## 🤖 AI 智能客服实现

本系统采用 **三级回复机制**，结合本地知识库快速响应与大语言模型深度理解能力：

### 回复流程

```
用户输入（AiChat.vue / chat/index.uvue）
        │
        ▼
1️⃣ 本地知识库匹配（knowledge-base/*.txt）
   ├─ 关键字快速匹配（价格/流程/押金/设备/物流/空域 ...）
   ├─ 响应 < 100ms，无需网络调用
   └─ 命中 → 直接返回（优先生效）
        │
        ▼（未命中）
2️⃣ Spring AI + 通义千问 LLM（qwen-turbo）
   ├─ 系统 Prompt 约束 + 对话上下文
   ├─ 响应 ~1.5s
   └─ 回复 → 返回用户
        │
        ▼（网络/API 失败）
3️⃣ 兜底模板消息
   └─ 友好提示用户稍后重试或联系客服
        │
        ▼
✅ 结果返回用户，并自动保存到 ai_chat_message 表供审计分析
```

### 配置方式（`application.yml`）

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

> 💡 任何 OpenAI 兼容接口均可使用（DeepSeek、Ollama 本地模型等）。无 API Key 时仍可使用本地知识库的"降级模式"。

### 本地知识库目录

```
backend/src/main/resources/knowledge-base/
├── drone_rental_rules.txt           # 租赁规则/流程
├── civil_aviation_regulations.txt   # 民航相关法规
├── airspace_application_process.txt # 空域备案流程
└── drone_insurance_regulations.txt  # 保险说明
```

可在管理端「AI 对话审计」持续补充命中不足的问题到知识库，实现 AI 的**自主学习闭环**。

---

## 📊 订单状态流转

```
          ┌──────────────┐
          │   创建订单    │
          └───────┬──────┘
                  │
                  ▼
         ┌────────────────┐
         │  待支付 (0)     │ ◄── 支付 / 取消
         └───────┬────────┘
                 │ 支付成功
                 ▼
         ┌────────────────┐
         │  待发货 (1)     │ ◄── 管理员发货 / 申请退款
         └───────┬────────┘
                 │ 发货
                 ▼
         ┌────────────────┐
         │  待收货 (2)     │ ◄── 确认收货 / 申请退款
         └───────┬────────┘
                 │ 确认收货
                 ▼
         ┌────────────────┐
         │  租赁中 (3)     │ ◄── 申请退租
         └───────┬────────┘
                 │ 归还
                 ▼
         ┌────────────────┐
         │  已归还 (4)     │ ◄── 发表评价
         └────────────────┘

    * 已取消 (5)：从待支付/已支付状态取消
    * 已退款 (6)：已支付订单退款
```

---

## 🔔 WebSocket 实时消息

后端在 `/ws/notification` 暴露 WebSocket 端点，支持以下**实时通知事件**：

| 事件 | 触发方 | 接收方 | 数据 |
|------|--------|--------|------|
| 新订单创建 | 用户下单 | 管理员 | `{ type: "new_order", orderId, orderNo, userName, totalAmount, ... }` |
| 订单状态变更 | 管理员/系统 | 用户 | `{ type: "order_status", orderId, status, ... }` |
| 故障报修提交 | 用户 | 管理员 | `{ type: "fault_report", faultId, ... }` |
| 空域备案提交 | 用户 | 管理员 | `{ type: "airspace_report", airspaceId, ... }` |
| 资质审核结果 | 管理员 | 用户 | `{ type: "qualification", auditStatus, ... }` |
| AI 新消息 | 系统 | 管理员（审计） | `{ type: "ai_message", ... }` |

**前端接入示例**（`utils/request.ts` 或组件内）：

```javascript
const token = uni.getStorageSync('token')
const ws = new WebSocket(`ws://localhost:8080/api/ws/notification?token=${token}`)

ws.onopen = () => console.log('✅ WebSocket 已连接')
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  console.log('📬 实时消息:', data)
  // TODO: unreadCount++ + 顶部 Toast 提示 + 刷新列表
}
ws.onerror = (err) => console.warn('❌ WS 错误', err)
ws.onclose = () => console.log('🔌 WS 断开，可重连')
```

---

## 🐳 一键启动（Docker 可选）

```bash
# 1. 启动 MySQL（可选，如未本地安装）
docker run -d --name drone-mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=drone_rental \
  -p 3306:3306 mysql:8.0

# 2. 导入数据
docker exec -i drone-mysql mysql -uroot -p123456 drone_rental < backend/sql/init.sql

# 3. 后端打包并运行
cd backend && mvn clean package -DskipTests
java -jar target/drone-rental-*.jar

# 4. 前端打包
cd frontend-web && npm run build
# 将 dist 部署到 Nginx / Vercel / OSS 静态托管
```

---

## 📱 移动端 / 小程序支持

UniApp 端与 Web 端共用同一套后端 API，支持以下编译目标：

| 目标平台 | 命令 / 操作 | 说明 |
|---------|------------|------|
| H5 | `npm run dev:h5` 或 HBuilderX「运行到浏览器」 | 与 Web 端体验一致，可独立部署 |
| 微信小程序 | HBuilderX「运行 → 运行到小程序模拟器 → 微信开发者工具」 | 需要 appid 与小程序合法域名 |
| App（iOS/Android） | HBuilderX「发行 → 原生 App-云打包」 | 提供签名证书即可生成安装包 |

> 开发文档参见 [`docs/UniApp微信小程序开发文档.md`](./docs/UniApp微信小程序开发文档.md)

---

## 📝 开发规范

### 代码风格

| 规则 | 前端（TS） | 后端（Java 21） |
|------|-----------|----------------|
| 组件/类 | PascalCase | PascalCase |
| 变量/方法 | camelCase | camelCase |
| 常量 | UPPER_SNAKE_CASE | UPPER_SNAKE_CASE |
| 状态管理 | Pinia `useXxx` | Spring Bean + @Service |
| 函数风格 | Hooks（组合式 API）、避免 class | 面向接口编程 |
| 字符串 | 模板字符串 | `String.format` / 文本块 `"""` |
| null 处理 | `??` / `?.` 链 | Optional / Objects.requireNonNull |

### Git 提交规范

```
feat:     新功能
fix:      修复 bug
docs:     文档
style:    格式调整（不影响代码运行）
refactor: 重构（不是新增也不是改 bug）
test:     增加/修改测试
chore:    构建过程或辅助工具变动
perf:     性能优化
revert:   回滚之前的提交
```

**示例：**

```
feat(user): 新增用户余额充值接口
fix(order): 修复取消订单时未恢复库存的问题
docs(readme): 完善快速开始章节
```

---

## ❓ 常见问题 FAQ

<details>
<summary><b>Q1：后端启动提示数据库连接失败？</b></summary>

1. 确认 MySQL 已启动：`netstat -anp | grep 3306`（Linux）/ 服务面板（Windows）
2. 确认已创建 `drone_rental` 数据库并导入 `backend/sql/init.sql`
3. 检查 `application.yml` 中 `username` / `password` 是否正确
4. 时区报错：确保 `serverTimezone=Asia/Shanghai` 或 `UTC`

</details>

<details>
<summary><b>Q2：AI 智能客服不回复或回复模板消息？</b></summary>

1. 检查 `application.yml` 中 `spring.ai.openai.api-key` 是否配置
2. 确认网络可访问 `dashscope.aliyuncs.com`
3. 若完全离线，可在管理端「AI 管理」关闭 LLM，使用纯本地知识库
4. 观察后端日志 `DroneRentalAgent` 或 `RagService` 报错信息

</details>

<details>
<summary><b>Q3：前端页面请求 API 返回 401 未授权？</b></summary>

- 确认已登录（Web 端：页面右上角，UniApp 端：个人中心）
- 确认 `request.ts` 中 `Authorization: Bearer {token}` 正确携带
- JWT 默认 2 小时过期，过期后需重新登录

</details>

<details>
<summary><b>Q4：小程序真机调试提示跨域 / 合法域名校验失败？</b></summary>

- 在微信开发者工具「项目设置」勾选**不校验合法域名、HTTPS 证书**（开发期）
- 生产环境需在微信公众平台配置 `request` 合法域名为你的后端域名

</details>

<details>
<summary><b>Q5：无人机图片 / 头像上传后页面不显示？</b></summary>

- 后端静态资源通过 `/api/uploads/xxx.png` 暴露
- 确认 `WebMvcConfig.addResourceHandlers` 已映射 `uploads/**` 和 `/api/uploads/**`
- H5 端走 Vite 代理，直连后端 `http://localhost:8080/api/uploads/...` 可验证

</details>

<details>
<summary><b>Q6：创建订单提示"请先完善飞行资质"？</b></summary>

- 需要先在个人中心 → 飞行资质 → 上传资质证明并等待审核
- 管理端在「资质审核」中通过后，方可下单
- 测试账号 `lisi`、`zhangxi` 已预置通过审核的资质，可直接下单测试

</details>

---

## 🗺 路线图

- [x] **v1.0** — 基础租赁流程（浏览/下单/支付/取消）
- [x] **v2.0** — 订单状态分类独立页面 + 评价系统 + 响应式
- [x] **v3.0** — AI 智能客服 + 空域备案 + 故障报修 + 消息通知 + 维保 + AI 对话审计
- [ ] **v4.0（计划）** — 支付宝/微信真实支付对接、地图定位、信用评分模型
- [ ] **v5.0（计划）** — 推荐算法优化、多租户 SaaS 架构、国际化（i18n）

---

## 📄 许可证

本项目采用 **MIT License** — 你可以自由使用、修改、分发，但需保留版权声明。详情见 [LICENSE](./LICENSE)。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

```bash
# 1. Fork 本仓库
# 2. 克隆你 fork 的仓库
git clone https://github.com/<your-name>/AI-Rental.git
cd AI-Rental

# 3. 创建特性分支
git checkout -b feature/your-amazing-feature

# 4. 开发并提交
git commit -m 'feat: add some amazing feature'

# 5. 推送分支
git push origin feature/your-amazing-feature

# 6. 打开 Pull Request
```

---

## 📮 联系方式

- **项目地址**：[https://github.com/SpringAI1/AI-Rental](https://github.com/SpringAI1/AI-Rental)
- **客服电话**：400-800-8888
- **支持邮箱**：support@drone-rental.com

---

<div align="center">

**如果本项目对你有帮助，请给一个 ⭐ Star 支持，谢谢！**

Made with ❤️ by SpringAI Team

</div>
