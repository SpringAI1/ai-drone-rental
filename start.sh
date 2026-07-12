#!/bin/bash
# ============================================================
# 一键启动后端 + Cloudflare Tunnel
# 使用方法：bash start.sh
# 前提：Mac 已开机，网络正常
# ============================================================

set -e

PROJECT_DIR="/Users/chenxi/Desktop/AI-Rental"
BACKEND_DIR="$PROJECT_DIR/backend"
JAR_PATH="$BACKEND_DIR/target/drone-rental-1.0.0.jar"
PORT=8081
LOG_FILE="/tmp/backend.log"
TUNNEL_LOG="/tmp/cloudflared.log"
CLOUDFLARED="/opt/homebrew/bin/cloudflared"

# CORS 白名单（Vercel 前端域名 + 本地开发）
CORS_ORIGINS="http://localhost:5173,http://localhost:5174,http://localhost:3000,http://localhost:8080,https://frontend-web-umber-six.vercel.app,https://frontend-ki3qa6u4e-hiioe181516.vercel.app,https://frontend-uniapp.vercel.app"

echo "=========================================="
echo "  AI-Rental 后端一键启动脚本"
echo "=========================================="

# ---------- 1. 检查 Java ----------
if ! /usr/libexec/java_home -v 20 >/dev/null 2>&1; then
  echo "[错误] 未找到 Java 20，请先安装 JDK 20"
  exit 1
fi
JAVA_HOME=$(/usr/libexec/java_home -v 20)
echo "[1/5] Java 20 ✓"

# ---------- 2. 检查 JAR 包 ----------
if [ ! -f "$JAR_PATH" ]; then
  echo "[2/5] JAR 包不存在，开始构建..."
  MVN="/tmp/apache-maven-3.9.16/bin/mvn"
  if [ ! -f "$MVN" ]; then
    echo "  下载 Maven..."
    curl -fsSL -o /tmp/maven.tar.gz "https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz"
    cd /tmp && tar xzf maven.tar.gz
  fi
  cd "$BACKEND_DIR" && "$MVN" clean package -DskipTests -q
  echo "  构建完成 ✓"
else
  echo "[2/5] JAR 包已存在 ✓"
fi

# ---------- 3. 检查端口占用，杀掉旧进程 ----------
echo "[3/5] 检查端口 $PORT..."
OLD_PID=$(lsof -ti :$PORT 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
  echo "  端口 $PORT 被占用 (PID $OLD_PID)，正在停止..."
  kill -9 $OLD_PID 2>/dev/null || true
  sleep 2
fi

# 杀掉旧的 cloudflared 进程
OLD_TUNNEL=$(pgrep -f "cloudflared tunnel --url" 2>/dev/null || true)
if [ -n "$OLD_TUNNEL" ]; then
  echo "  停止旧 Tunnel 进程..."
  kill $OLD_TUNNEL 2>/dev/null || true
  sleep 1
fi

# ---------- 4. 启动后端 ----------
echo "[4/5] 启动后端 (端口 $PORT)..."
cd "$BACKEND_DIR"
JAVA_HOME=$JAVA_HOME nohup java -jar "$JAR_PATH" \
  --server.port=$PORT \
  --file.upload-path=../uploads/ \
  --cors.allowed-origins="$CORS_ORIGINS" \
  > "$LOG_FILE" 2>&1 &
BACKEND_PID=$!
echo "  后端 PID: $BACKEND_PID"

# 等待后端启动
echo "  等待后端启动..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w '' "http://localhost:$PORT/api/drone/list?page=1&size=1" 2>/dev/null; then
    echo "  后端启动成功 ✓"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "  [错误] 后端启动超时，请检查日志: $LOG_FILE"
    exit 1
  fi
  sleep 2
done

# ---------- 5. 启动 Cloudflare Tunnel ----------
echo "[5/5] 启动 Cloudflare Tunnel..."
if [ ! -f "$CLOUDFLARED" ]; then
  echo "  [错误] cloudflared 未安装"
  exit 1
fi

nohup "$CLOUDFLARED" tunnel --url "http://localhost:$PORT" > "$TUNNEL_LOG" 2>&1 &
TUNNEL_PID=$!
echo "  Tunnel PID: $TUNNEL_PID"

# 等待 Tunnel URL 出现
echo "  等待 Tunnel URL..."
TUNNEL_URL=""
for i in $(seq 1 20); do
  TUNNEL_URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$TUNNEL_LOG" 2>/dev/null | head -1)
  if [ -n "$TUNNEL_URL" ]; then
    echo "  Tunnel 启动成功 ✓"
    break
  fi
  sleep 2
done

if [ -z "$TUNNEL_URL" ]; then
  echo "  [错误] Tunnel URL 获取超时，请检查日志: $TUNNEL_LOG"
  exit 1
fi

# ---------- 输出结果 ----------
echo ""
echo "=========================================="
echo "  启动完成！"
echo "=========================================="
echo ""
echo "  后端 API:   $TUNNEL_URL/api"
echo "  后端日志:   $LOG_FILE"
echo "  Tunnel日志: $TUNNEL_LOG"
echo ""
echo "  前端地址:"
echo "    Web:     https://frontend-web-umber-six.vercel.app"
echo "    uniapp:  https://frontend-uniapp.vercel.app"
echo ""
echo "  ⚠️  Tunnel URL 变了的话，需要更新 Vercel 环境变量："
echo "      VITE_API_BASE_URL = $TUNNEL_URL/api"
echo "      然后在 Vercel 重新部署前端"
echo ""
echo "  按 Ctrl+C 停止后端和 Tunnel..."
echo ""

# 等待 Ctrl+C
trap "echo ''; echo '正在停止...'; kill $BACKEND_PID $TUNNEL_PID 2>/dev/null; echo '已停止'; exit 0" INT TERM
wait
