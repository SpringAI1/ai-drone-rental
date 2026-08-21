# 云服务器部署指南

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 20+ | 前端构建 |
| Redis | 7+ | 缓存（可选，本地测试可禁用） |
| Nginx | 1.18+ | 前端静态服务 + 反向代理 |

## 一、后端部署

### 1.1 打包

```bash
cd backend
mvn clean package -DskipTests
# 产物位置：backend/target/drone-rental-1.0.0.jar
```

### 1.2 上传并启动

```bash
# 上传 JAR 到服务器
scp target/drone-rental-1.0.0.jar root@<服务器IP>:/opt/drone-rental/

# 上传数据库（如果需要保留数据）
scp drone_rental.db root@<服务器IP>:/opt/drone-rental/

# SSH 到服务器
ssh root@<服务器IP>

# 设置环境变量
cat >> ~/.bashrc << 'EOF'
export DASHSCOPE_API_KEY=sk-your-key-here
export JWT_SECRET=$(openssl rand -base64 32)
export SQLITE_PATH=/opt/drone-rental/drone_rental.db
EOF
source ~/.bashrc

# 启动后端
cd /opt/drone-rental
nohup java -jar drone-rental-1.0.0.jar > app.log 2>&1 &

# 验证启动
curl http://localhost:8080/api/drone/list
```

### 1.3 使用 systemd 管理（推荐）

```bash
cat > /etc/systemd/system/drone-rental.service << 'EOF'
[Unit]
Description=Drone Rental Backend
After=network.target redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/drone-rental
Environment="DASHSCOPE_API_KEY=sk-your-key"
Environment="JWT_SECRET=your-random-secret"
Environment="SQLITE_PATH=/opt/drone-rental/drone_rental.db"
ExecStart=/usr/bin/java -jar /opt/drone-rental/drone-rental-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable drone-rental
systemctl start drone-rental
```

## 二、前端部署

### 2.1 打包

```bash
cd frontend-web
npm run build
# 产物位置：frontend-web/dist/
```

### 2.2 上传静态文件

```bash
scp -r dist/* root@<服务器IP>:/var/www/drone-rental/
```

### 2.3 配置 Nginx

```bash
# 将 deploy/nginx.conf 复制到服务器
scp deploy/nginx.conf root@<服务器IP>:/etc/nginx/conf.d/drone-rental.conf

# SSH 到服务器测试并重载
ssh root@<服务器IP>
nginx -t
nginx -s reload
```

### 2.4 验证部署

```bash
# 访问前端
curl http://<服务器IP>/

# 访问 API（通过 Nginx 代理）
curl http://<服务器IP>/api/drone/list
```

## 三、Docker Compose 一键部署（推荐）

在项目根目录创建 `docker-compose.yml`：

```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    restart: always
    ports:
      - "6379:6379"

  backend:
    build: ./backend
    restart: always
    ports:
      - "8080:8080"
    environment:
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - JWT_SECRET=${JWT_SECRET:-changeme}
      - SQLITE_PATH=/data/drone_rental.db
      - REDIS_HOST=redis
    volumes:
      - ./data:/data

  nginx:
    image: nginx:alpine
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./deploy/nginx.conf:/etc/nginx/conf.d/default.conf
      - ./frontend-web/dist:/var/www/drone-rental
    depends_on:
      - backend
```

```bash
# 启动所有服务
docker-compose up -d
```

## 四、域名 + HTTPS 配置

### 4.1 申请 SSL 证书（Let's Encrypt）

```bash
# 安装 certbot
apt install certbot python3-certbot-nginx

# 申请证书
certbot --nginx -d <你的域名>
```

### 4.2 启用 Nginx HTTPS 配置

编辑 `/etc/nginx/conf.d/drone-rental.conf`，取消注释 HTTPS server 块，重启 Nginx。

## 五、常见问题

**Q: 后端启动后 API 返回 500？**
A: 检查数据库文件路径是否正确，`SQLITE_PATH` 环境变量需指向实际 .db 文件位置。

**Q: AI 对话无响应？**
A: 确认 `DASHSCOPE_API_KEY` 环境变量已设置且 Key 有效。

**Q: 前端页面刷新后 404？**
A: 检查 Nginx 配置中 `try_files $uri $uri/ /index.html;` 是否存在（Vue Router history 模式必需）。

**Q: WebSocket 连接失败？**
A: 确认 Nginx 中 `/api/ws/` 路径配置了 `Upgrade` 和 `Connection` 头。
