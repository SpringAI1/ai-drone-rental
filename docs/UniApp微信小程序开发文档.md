# 无人机租赁系统 - UniApp/微信小程序开发文档

## 一、项目概述

本文档用于指导UniApp（App和小程序）和微信小程序端的开发，确保移动端与Web端保持一致的样式和功能逻辑，共用同一个后端API。

### 1.1 技术选型

| 平台 | 技术栈 |
|------|--------|
| **Web端（已有）** | Vue 3 + Vite + Element Plus |
| **UniApp App端** | Vue 3 + uni-app + uni-ui |
| **微信小程序** | 原生开发或uni-app跨平台 |
| **后端** | Spring Boot + MyBatis-Plus + JWT |

### 1.2 后端服务地址

```
生产环境: http://your-domain.com/api
开发环境: http://localhost:8080/api
```

### 1.3 认证方式

- 使用 **JWT Token** 进行身份认证
- Token 在登录成功后返回，存储在客户端
- 所有需要认证的请求需要在 Header 中携带：
  ```
  Authorization: Bearer <token>
  ```

---

## 二、项目结构

### 2.1 UniApp项目结构建议

```
drone-rental-uniapp/
├── pages/                    # 页面
│   ├── index/               # 首页
│   ├── drones/              # 设备列表
│   ├── drone-detail/        # 设备详情
│   ├── orders/              # 订单相关
│   │   ├── list/            # 订单列表
│   │   ├── detail/          # 订单详情
│   │   ├── pay/             # 订单支付
│   │   └── evaluate/         # 订单评价
│   ├── user/                # 用户相关
│   │   ├── profile/         # 个人中心
│   │   ├── login/           # 登录
│   │   └── register/        # 注册
│   ├── airspace/             # 空域备案
│   ├── chat/                 # AI客服
│   └── notification/          # 消息通知
├── components/               # 组件
├── api/                     # API请求封装
├── stores/                  # 状态管理
├── utils/                   # 工具函数
└── static/                  # 静态资源
```

### 2.2 微信小程序项目结构建议

```
drone-rental-miniprogram/
├── pages/
├── components/
├── api/
├── utils/
└── app.js
```

---

## 三、API接口文档

### 3.1 基础响应格式

所有接口返回统一格式：

```json
{
  "code": 200,           // 状态码，200表示成功
  "message": "操作成功",  // 消息
  "data": {},            // 数据体
  "timestamp": 1781850000,
  "success": true
}
```

**常见错误码：**
| code | 说明 |
|------|------|
| 200 | 成功 |
| 401 | 未授权（Token过期或无效）|
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

### 3.2 认证模块 `/auth`

#### 3.2.1 用户登录
```
POST /auth/login
Content-Type: application/json

请求体：
{
  "username": "admin",
  "password": "123456"
}

响应：
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "role": 1,           // 1=管理员, 2=普通用户
    "token": "eyJhbGci..."
  }
}
```

#### 3.2.2 用户注册
```
POST /auth/register
Content-Type: application/json

请求体：
{
  "username": "newuser",
  "password": "123456",
  "phone": "13800138000",
  "email": "user@example.com"
}

响应：{ "code": 200, "message": "注册成功" }
```

#### 3.2.3 管理员登录
```
POST /auth/admin/login
Content-Type: application/json

请求体：
{
  "username": "admin",
  "password": "123456"
}

响应：同用户登录
```

---

### 3.3 首页模块 `/public`

#### 3.3.1 获取首页统计数据
```
GET /public/stats
无需认证（公开接口）

响应：
{
  "code": 200,
  "data": {
    "totalDrones": 10,
    "totalUsers": 100,
    "totalOrders": 500,
    "totalRevenue": 150000.00
  }
}
```

---

### 3.4 用户模块 `/user`（需认证）

#### 3.4.1 获取当前用户信息
```
GET /user/info
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "phone": "13800138000",
    "email": "admin@example.com",
    "avatar": "/uploads/avatar.png",
    "balance": 1000.00,
    "creditScore": 100,
    "role": 1,
    "qualificationStatus": 1  // 0=未提交, 1=审核中, 2=已通过, 3=已拒绝
  }
}
```

#### 3.4.2 更新用户信息
```
PUT /user/info
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "nickname": "新昵称",
  "phone": "13800138000",
  "email": "new@example.com"
}

响应：{ "code": 200, "message": "更新成功" }
```

#### 3.4.3 修改密码
```
PUT /user/password
Authorization: Bearer <token>

请求参数：
- oldPassword: 原密码
- newPassword: 新密码

响应：{ "code": 200, "message": "密码修改成功" }
```

#### 3.4.4 用户充值
```
POST /user/recharge
Authorization: Bearer <token>

请求参数：
- amount: 充值金额（BigDecimal）

响应：{ "code": 200, "message": "充值成功" }
```

#### 3.4.5 获取当前用户飞行资质
```
GET /user/qualification
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 1,
    "licenseType": "AOPA",
    "licenseNumber": "123456789",
    "certificateImages": ["/uploads/cert1.jpg"],
    "status": 2,
    "submitTime": "2026-06-01T10:00:00",
    "auditTime": "2026-06-02T15:30:00"
  }
}
```

---

### 3.5 订单模块

#### 3.5.1 用户订单列表 `/user/orders`（需认证）
```
GET /user/orders?pageNum=1&pageSize=10&orderStatus=0
Authorization: Bearer <token>

参数：
- pageNum: 页码（默认1）
- pageSize: 每页数量（默认10）
- orderStatus: 订单状态筛选（可选）
  - 0: 待支付
  - 1: 待发货
  - 2: 待收货
  - 3: 租赁中
  - 4: 已归还
  - 5: 已取消/已退款

响应：
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "orderNo": "ORD17816921141847B3F7A",
        "userId": 1,
        "username": "系统管理员",
        "droneId": 1,
        "droneModel": "DJI Mavic 3",
        "droneImage": "/uploads/mavic3_drone.png",
        "rentalStartTime": "2026-06-20T00:00:00",
        "rentalEndTime": "2026-06-23T23:59:59",
        "rentalDays": 4,
        "unitPrice": 299.00,
        "totalAmount": 1196.00,
        "depositAmount": 0.00,
        "orderStatus": 0,
        "orderStatusDesc": "待支付",
        "hasComment": false,
        "createdTime": "2026-06-17T18:28:34"
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

#### 3.5.2 获取订单统计 `/user/order-stats`（需认证）
```
GET /user/order-stats
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "pendingPay": 2,      // 待支付
    "pendingShip": 0,      // 待发货
    "pendingReceive": 0,   // 待收货
    "renting": 0,          // 租赁中
    "returned": 1,         // 已归还
    "canceled": 1          // 已取消/退款
  }
}
```

#### 3.5.3 获取订单详情 `/order/{orderId}`（需认证）
```
GET /order/{orderId}
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "id": 1,
    "orderNo": "ORD17816921141847B3F7A",
    "userId": 1,
    "username": "系统管理员",
    "phone": "13800138000",
    "droneId": 1,
    "droneModel": "DJI Mavic 3",
    "droneImage": "/uploads/mavic3_drone.png",
    "airspaceRecordId": null,
    "regionName": null,
    "rentalStartTime": "2026-06-20T00:00:00",
    "rentalEndTime": "2026-06-23T23:59:59",
    "rentalDays": 4,
    "unitPrice": 299.00,
    "totalAmount": 1196.00,
    "depositAmount": 0.00,
    "orderStatus": 0,
    "orderStatusDesc": "待支付",
    "deliveryAddress": null,
    "remark": null,
    "cancelReason": null,
    "refundReason": null,
    "hasComment": false,
    "createdTime": "2026-06-17T18:28:34",
    "payTime": null,
    "shipTime": null,
    "receiveTime": null,
    "returnTime": null,
    "cancelTime": null
  }
}
```

#### 3.5.4 创建订单 `/order/create`（需认证）
```
POST /order/create
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "droneId": 1,
  "rentalStartTime": "2026-06-20",
  "rentalEndTime": "2026-06-23",
  "airspaceRecordId": null,
  "deliveryAddress": "北京市朝阳区xxx",
  "remark": "需要配送"
}

响应：
{
  "code": 200,
  "data": {
    "id": 10,
    "orderNo": "ORD17818500000000001",
    "totalAmount": 1196.00
  }
}
```

#### 3.5.5 支付订单 `/order/{orderId}/pay`（需认证）
```
POST /order/{orderId}/pay
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "paymentMethod": "balance"  // balance=余额, wechat=微信, alipay=支付宝
}

响应：{ "code": 200, "message": "支付成功" }
```

#### 3.5.6 取消订单 `/order/{orderId}/cancel`（需认证）
```
POST /order/{orderId}/cancel
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "reason": "不想租了"
}

响应：{ "code": 200, "message": "订单已取消" }
```

#### 3.5.7 确认收货 `/order/{orderId}/receive`（需认证）
```
POST /order/{orderId}/receive
Authorization: Bearer <token>

响应：{ "code": 200, "message": "确认收货成功" }
```

#### 3.5.8 申请退租 `/order/{orderId}/return`（需认证）
```
POST /order/{orderId}/return
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "returnAddress": "北京市朝阳区xxx"
}

响应：{ "code": 200, "message": "申请退租成功" }
```

#### 3.5.9 申请退款 `/order/{orderId}/refund`（需认证）
```
POST /order/{orderId}/refund
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "reason": "计划变更，需要退款"
}

响应：{ "code": 200, "message": "退款申请已提交" }
```

---

### 3.6 设备模块 `/drone`

#### 3.6.1 获取设备列表
```
GET /drone/list?pageNum=1&pageSize=10&keyword=Mavic&status=1&minPrice=0&maxPrice=5000
Authorization: Bearer <token>（可选，不传则只返回上架设备）

参数：
- pageNum: 页码
- pageSize: 每页数量
- keyword: 关键词搜索
- status: 状态（1=上架）
- brand: 品牌筛选
- type: 类型筛选
- minPrice/maxPrice: 价格区间

响应：
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "model": "DJI Mavic 3",
        "brand": "DJI",
        "type": "消费级",
        "image": "/uploads/mavic3_drone.png",
        "price": 299.00,
        "deposit": 500.00,
        "stock": 5,
        "status": 1,
        "description": "专业航拍无人机",
        "specifications": {
          "weight": "895g",
          "maxSpeed": "19m/s",
          "maxAltitude": "6000m",
          "batteryLife": "46分钟"
        }
      }
    ],
    "total": 10,
    "current": 1
  }
}
```

#### 3.6.2 获取设备详情
```
GET /drone/detail/{id}
Authorization: Bearer <token>（可选）

响应：
{
  "code": 200,
  "data": {
    "id": 1,
    "model": "DJI Mavic 3",
    "brand": "DJI",
    "type": "消费级",
    "image": "/uploads/mavic3_drone.png",
    "images": ["/uploads/mavic3_1.jpg", "/uploads/mavic3_2.jpg"],
    "price": 299.00,
    "deposit": 500.00,
    "stock": 5,
    "status": 1,
    "description": "专业航拍无人机",
    "specifications": {
      "weight": "895g",
      "maxSpeed": "19m/s",
      "maxAltitude": "6000m",
      "batteryLife": "46分钟"
    },
    "rentalCount": 156,
    "rating": 4.8
  }
}
```

#### 3.6.3 获取设备评论列表
```
GET /drone/{droneId}/comments?pageNum=1&pageSize=10
Authorization: Bearer <token>（可选）

响应：
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "userNickname": "用户1",
        "userAvatar": "/uploads/avatar1.png",
        "content": "非常棒的无人机！",
        "rating": 5,
        "images": ["/uploads/comment1.jpg"],
        "children": [
          {
            "id": 2,
            "userId": 2,
            "userNickname": "商家",
            "userAvatar": "/uploads/admin.png",
            "content": "感谢好评！",
            "parentId": 1,
            "createTime": "2026-06-18T10:00:00"
          }
        ],
        "createTime": "2026-06-17T15:30:00"
      }
    ],
    "total": 10
  }
}
```

#### 3.6.4 获取品牌列表
```
GET /drone/brands
Authorization: Bearer <token>（可选）

响应：
{
  "code": 200,
  "data": ["DJI", "Autel", "Parrot"]
}
```

#### 3.6.5 获取类型列表
```
GET /drone/types
Authorization: Bearer <token>（可选）

响应：
{
  "code": 200,
  "data": ["消费级", "专业级", "行业级"]
}
```

---

### 3.7 评论模块 `/comment`（需认证）

#### 3.7.1 添加评论
```
POST /comment/add
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "droneId": 1,
  "orderId": 1,
  "content": "非常棒的无人机！",
  "rating": 5,
  "images": ["/uploads/comment1.jpg"],
  "parentId": null  // 回复评论时传入父评论ID
}

响应：{ "code": 200, "message": "评论成功" }
```

#### 3.7.2 获取我的评论
```
GET /comment/my?pageNum=1&pageSize=10
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 5
  }
}
```

#### 3.7.3 删除评论
```
DELETE /comment/{id}
Authorization: Bearer <token>

响应：{ "code": 200, "message": "删除成功" }
```

---

### 3.8 空域备案模块 `/airspace`（需认证）

#### 3.8.1 提交空域备案
```
POST /airspace/submit
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "regionName": "测试区域",
  "regionRange": "北京市朝阳区",
  "maxAltitude": 120,
  "startTime": "2026-06-20 08:00:00",
  "endTime": "2026-06-20 18:00:00",
  "remark": "测试备注"
}

响应：{ "code": 200, "message": "提交成功" }
```

#### 3.8.2 获取我的空域备案列表
```
GET /airspace/list
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "regionName": "测试区域",
      "regionRange": "北京市朝阳区",
      "maxAltitude": 120,
      "startTime": "2026-06-20 08:00:00",
      "endTime": "2026-06-20 18:00:00",
      "auditStatus": 1,  // 0=待审核, 1=已通过, 2=已拒绝
      "auditRemark": null,
      "createdTime": "2026-06-17T10:00:00"
    }
  ]
}
```

---

### 3.9 故障报修模块 `/user/fault`（需认证）

#### 3.9.1 提交故障报修
```
POST /user/fault/report
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "orderId": 1,
  "faultType": "电池故障",
  "description": "电池无法充电",
  "images": ["/uploads/fault1.jpg"]
}

响应：{ "code": 200, "message": "提交成功" }
```

#### 3.9.2 获取我的故障报修列表
```
GET /user/fault/list
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": [...]
}
```

---

### 3.10 消息通知模块 `/notification`（需认证）

#### 3.10.1 获取通知列表
```
GET /notification/list?pageNum=1&pageSize=20
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "type": 1,  // 1=订单消息, 2=系统消息, 3=审核消息
        "title": "订单支付成功",
        "content": "您的订单已支付成功",
        "readStatus": 0,
        "createdTime": "2026-06-17T15:30:00"
      }
    ],
    "total": 10
  }
}
```

#### 3.10.2 获取未读数量
```
GET /notification/unread-count
Authorization: Bearer <token>

响应：
{
  "code": 200,
  "data": 5
}
```

#### 3.10.3 标记已读
```
PUT /notification/{id}/read
Authorization: Bearer <token>

响应：{ "code": 200, "message": "操作成功" }
```

#### 3.10.4 全部已读
```
PUT /notification/read-all
Authorization: Bearer <token>

响应：{ "code": 200, "message": "操作成功" }
```

---

### 3.11 AI客服模块 `/ai/v2`（需认证）

#### 3.11.1 发送消息
```
POST /ai/v2/chat
Authorization: Bearer <token>
Content-Type: application/json

请求体：
{
  "message": "我想租一架无人机",
  "conversationId": "uuid-xxx"  // 可选，用于多轮对话
}

响应：
{
  "code": 200,
  "data": {
    "reply": "您好！请问您想租什么类型的无人机？",
    "conversationId": "uuid-xxx",
    "tools": [...]  // 如果有调用工具，返回工具调用结果
  }
}
```

---

## 四、页面功能对照表

### 4.1 用户端页面

| Web端页面 | UniApp/小程序页面 | 功能说明 |
|-----------|------------------|---------|
| Home.vue | pages/index/index | 首页，展示统计、设备推荐 |
| DroneList.vue | pages/drones/list | 设备列表，支持搜索筛选 |
| DroneDetail.vue | pages/drone-detail/index | 设备详情，评论展示 |
| MyOrders.vue | pages/orders/list | 订单首页，状态统计卡片 |
| OrderPendingPay.vue | - (合并到订单列表) | 待支付订单列表 |
| OrderPendingShip.vue | - | 待发货订单列表 |
| OrderPendingReceive.vue | - | 待收货订单列表 |
| OrderRenting.vue | - | 租赁中订单列表 |
| OrderReturned.vue | - | 已归还订单列表 |
| OrderCanceled.vue | - | 已取消订单列表 |
| OrderDetail.vue | pages/orders/detail | 订单详情 |
| OrderPay.vue | pages/orders/pay | 订单支付 |
| Profile.vue | pages/user/profile | 个人中心 |
| AirspaceRecord.vue | pages/airspace/record | 空域备案 |
| Chat.vue | pages/chat/index | AI客服 |
| FaultReport.vue | pages/fault/report | 故障报修 |

---

## 五、样式适配建议

### 5.1 响应式布局

由于移动端屏幕较小，建议采用以下策略：

1. **使用Flex布局**：弹性布局适配不同屏幕
2. **单位使用rpx**：UniApp推荐使用rpx单位自动适配
3. **组件库选择**：
   - UniApp: `uni-ui`、`uView`
   - 微信小程序: `Vant Weapp`、`WeUI`

### 5.2 颜色主题

保持与Web端一致的主题色：

```css
/* 主色调 */
--primary-color: #409eff;     /* Element Plus 蓝 */
/* 成功色 */
--success-color: #67c23a;
/* 警告色 */
--warning-color: #e6a23c;
/* 危险色 */
--danger-color: #f56c6c;
/* 信息色 */
--info-color: #909399;
```

### 5.3 图标

Web端使用 Element Plus Icons，移动端建议使用：
- UniApp: `uni-icons`
- 微信小程序: `iconfont` 或 `weui-icon`

---

## 六、开发注意事项

### 6.1 Token管理

```javascript
// 保存Token
uni.setStorageSync('token', response.data.token);

// 请求时携带Token
uni.request({
  url: BASE_URL + '/user/info',
  header: {
    'Authorization': 'Bearer ' + uni.getStorageSync('token')
  }
});
```

### 6.2 登录拦截

```javascript
// main.js 或 App.vue
uni.addInterceptor('request', {
  invoke(args) {
    // 拦截非登录请求，检查Token
    if (!uni.getStorageSync('token') && args.url.includes('/user')) {
      uni.showToast({ title: '请先登录', icon: 'none' });
      uni.navigateTo({ url: '/pages/user/login' });
      return false;
    }
    return args;
  }
});
```

### 6.3 支付流程

移动端支付需要调用对应平台的支付能力：

```javascript
// 微信小程序支付
uni.requestPayment({
  provider: 'wxpay',
  timeStamp: res.timeStamp,
  nonceStr: res.nonceStr,
  package: res.package,
  signType: res.signType,
  paySign: res.paySign,
  success: () => {
    // 支付成功
  }
});
```

### 6.4 图片上传

```javascript
uni.chooseImage({
  count: 9,
  success: (res) => {
    res.tempFiles.forEach(file => {
      uni.uploadFile({
        url: BASE_URL + '/upload',
        filePath: file.path,
        name: 'file',
        success: (uploadRes) => {
          // 获取返回的图片路径
        }
      });
    });
  }
});
```

---

## 七、项目初始化命令

### 7.1 UniApp项目创建

```bash
# 使用HBuilderX创建
# 文件 -> 新建 -> 项目 -> 选择uni-app -> 选择Vue版本

# 或使用CLI创建
npx degit dcloudio/uni-preset-vue#vite-ts my-uniapp
cd my-uniapp
npm install
```

### 7.2 微信小程序项目创建

```bash
# 使用微信开发者工具创建
# 新建项目 -> 选择uni-app项目类型
```

---

## 八、接口示意图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           移动端（UniApp/小程序）                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐          │
│  │  首页    │    │ 设备列表  │    │ 设备详情  │    │ 订单列表  │          │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘          │
│       │               │               │               │                  │
│       └───────────────┴───────────────┴───────────────┘                  │
│                                   │                                       │
│                                   ▼                                       │
│                    ┌──────────────────────────────┐                       │
│                    │        API 请求封装层          │                       │
│                    │   (request.js / api/index.js) │                       │
│                    └──────────────────────────────┘                       │
│                                   │                                       │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │ HTTP/HTTPS
                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                              后端 API (Spring Boot)                       │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                        认证模块 /auth                                │ │
│  │   POST /auth/login    - 用户登录                                     │ │
│  │   POST /auth/register - 用户注册                                     │ │
│  │   POST /auth/admin/login - 管理员登录                                 │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      公开接口 /public                                │ │
│  │   GET /public/stats - 首页统计数据                                   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      用户模块 /user (需认证)                          │ │
│  │   GET  /user/info       - 获取用户信息                                │ │
│  │   PUT  /user/info       - 更新用户信息                                │ │
│  │   GET  /user/orders     - 获取订单列表                                │ │
│  │   GET  /user/order-stats - 获取订单统计                               │ │
│  │   POST /user/recharge   - 用户充值                                   │ │
│  │   GET  /user/qualification - 获取飞行资质                             │ │
│  │   POST /user/fault/report - 故障报修                                  │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      订单模块 /order (需认证)                          │ │
│  │   POST /order/create     - 创建订单                                   │ │
│  │   GET  /order/{id}       - 订单详情                                   │ │
│  │   POST /order/{id}/pay   - 支付订单                                   │ │
│  │   POST /order/{id}/cancel - 取消订单                                  │ │
│  │   POST /order/{id}/receive - 确认收货                                 │ │
│  │   POST /order/{id}/return - 申请退租                                 │ │
│  │   POST /order/{id}/refund  - 申请退款                                 │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      设备模块 /drone                                  │ │
│  │   GET /drone/list      - 设备列表                                     │ │
│  │   GET /drone/detail/{id} - 设备详情                                  │ │
│  │   GET /drone/brands    - 品牌列表                                     │ │
│  │   GET /drone/types     - 类型列表                                    │ │
│  │   GET /drone/{id}/comments - 评论列表                                │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      评论模块 /comment (需认证)                       │ │
│  │   POST /comment/add    - 添加评论                                    │ │
│  │   GET  /comment/my     - 我的评论                                     │ │
│  │   DELETE /comment/{id} - 删除评论                                    │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      空域模块 /airspace (需认证)                      │ │
│  │   POST /airspace/submit - 提交备案                                   │ │
│  │   GET  /airspace/list  - 备案列表                                    │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                    消息模块 /notification (需认证)                    │ │
│  │   GET  /notification/list     - 通知列表                              │ │
│  │   GET  /notification/unread-count - 未读数量                        │ │
│  │   PUT  /notification/{id}/read - 标记已读                           │ │
│  │   PUT  /notification/read-all  - 全部已读                          │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                      AI模块 /ai/v2 (需认证)                          │ │
│  │   POST /ai/v2/chat    - AI对话                                       │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                            MySQL 数据库                                   │
├───────────────────────────────────────────────────────────────────────────┤
│  user | drone | rental_order | comment | notification |                   │
│  qualification | airspace_record | fault_report | maintenance_ticket      │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 九、订单状态流转图

```
     ┌──────────────────────────────────────────────────────────────┐
     │                                                              │
     │    ┌─────────┐                                               │
     │    │ 创建订单 │ ──────────────────────────────────────┐      │
     │    └────┬────┘                                       │      │
     │         │ 状态: 0                                    │      │
     │         ▼                                           │      │
     │    ┌─────────┐    支付成功                           │      │
     │    │ 待支付   │ ──────────────────────────────► ┌─────────┐ │
     │    └────┬────┘                                 │ 待发货   │ │
     │         │ 取消订单                               │ 状态: 1  │ │
     │         ▼                                       └────┬────┘ │
     │    ┌─────────┐                                       │      │
     │    │ 已取消   │                                       │      │
     │    └─────────┘                                       │      │
     │         ▲                                           │ 发货   │
     │         │                                           ▼      │
     │    ┌─────────┐                                 ┌─────────┐ │
     │    │ 已退款   │ ◄───────────────────────────── │ 待收货   │ │
     │    └─────────┘      退款/退货                   │ 状态: 2  │ │
     │                                                  └────┬────┘ │
     │                                                         │      │
     │                                                  确认收货 │      │
     │                                                         ▼      │
     │                                                  ┌─────────┐ │
     │                                                  │ 租赁中   │ │
     │                                                  │ 状态: 3  │ │
     │                                                  └────┬────┘ │
     │                                                         │      │
     │                                                  申请退租 │      │
     │                                                         ▼      │
     │                                                  ┌─────────┐ │
     │                                                  │ 已归还   │ │
     │                                                  │ 状态: 4  │ │
     │                                                  └─────────┘ │
     │                                                               │
     └───────────────────────────────────────────────────────────────┘
```

---

## 十、联系方式

如有问题，请联系：
- 后端技术负责人
- 邮箱：support@example.com
