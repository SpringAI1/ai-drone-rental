# AI 无人机租赁系统 - 项目总结

> 本文件夹包含项目的完整总结文档，用于快速了解项目全貌。

## 文档目录

| 文档 | 内容 |
|------|------|
| [01-项目概述.md](01-项目概述.md) | 项目简介、技术栈、代码规模、目录结构 |
| [02-后端架构.md](02-后端架构.md) | 后端技术栈、包结构、功能模块、数据库、安全特性 |
| [03-前端架构.md](03-前端架构.md) | Web 前端和 uniapp 页面清单、组件、路由 |
| [04-运行部署.md](04-运行部署.md) | 本地开发、生产部署、常见问题 |
| [05-功能清单.md](05-功能清单.md) | 完整功能列表（用户端 + 管理端 + 安全 + 技术特色） |

## 项目快速概览

### 基本信息
- **项目名称**：AI 无人机租赁系统
- **GitHub 仓库**：https://github.com/SpringAI1/ai-drone-rental
- **分支**：Health
- **代码规模**：252 个文件，45,311 行代码

### 技术栈
- **后端**：Spring Boot 3.2.5 + MyBatis-Plus + SQLite + JWT + Spring AI
- **Web 前端**：Vue 3 + Vite + Element Plus + Pinia + ECharts
- **移动端**：uni-app + Vue 3 + TypeScript（H5 + 微信小程序）

### 核心功能
1. 用户认证（JWT + BCrypt + IP 限流）
2. 无人机租赁全流程（浏览→下单→支付→发货→收货→归还→评价）
3. AI 智能客服（RAG + Function Calling + SSE 流式）
4. 管理后台（13 个管理页面）
5. 实时通知（WebSocket）
6. RSA+AES 请求加密

### 部署方式
- 前端：Vercel 静态托管
- 后端：Cloudflare Tunnel 内网穿透
- 数据库：SQLite
- 启动脚本：`bash start.sh`

### 访问地址
| 服务 | 地址 |
|------|------|
| Web 前端 | https://frontend-web-umber-six.vercel.app |
| uniapp H5 | https://frontend-uniapp.vercel.app |
| 后端 API | （每次启动变化，见 start.sh 输出） |
| Swagger 文档 | http://localhost:8080/api/swagger-ui/index.html |

### 默认账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
