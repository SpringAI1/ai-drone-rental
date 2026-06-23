#!/usr/bin/env python3
"""
全功能联调测试 - 模拟前端 H5 (localhost:3000) 调用，验证 CORS + 鉴权 + 业务全链路
"""
import json
import re
import sqlite3
import sys
import urllib.parse
import urllib.request
import urllib.error
from datetime import date, timedelta

BASE = "http://localhost:8080/api"
DB = "/Users/chenxi/Desktop/AI-Rental/backend/drone_rental.db"
HEADERS_BASE = {
    "Origin": "http://localhost:3000",  # 模拟 uniapp H5 端口
    "Content-Type": "application/json"
}

# 结果统计
passes = []
fails = []


def http(method, path, token=None, body=None, label=""):
    # URL 编码路径中的中文
    safe_path = urllib.parse.quote(path, safe="/?&=:")
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    req = urllib.request.Request(BASE + safe_path, data=data, method=method)
    for k, v in HEADERS_BASE.items():
        req.add_header(k, v)
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            code = r.status
            txt = r.read().decode("utf-8")
            try:
                return code, json.loads(txt)
            except Exception:
                return code, {"raw": txt}
    except urllib.error.HTTPError as e:
        body_text = e.read().decode("utf-8") or "{}"
        try:
            return e.code, json.loads(body_text)
        except Exception:
            return e.code, {"raw": body_text}


def check(label, code, expected=200, body=None):
    status = "✅" if code == expected else "❌"
    msg = f"{status} {label}: HTTP {code}"
    if code != expected:
        if body:
            msg += f" | {str(body)[:200]}"
        fails.append(label)
    else:
        passes.append(label)
    print(msg)


def section(title):
    print()
    print("=" * 70)
    print(f"【{title}】")
    print("=" * 70)


# =============================================================
# 1) 公共端点（无需登录）
# =============================================================
section("1. 公共端点（无需 JWT）")
code, d = http("GET", "/ai/v3/status")
check("GET /ai/v3/status", code, 200, d)

code, d = http("GET", "/ai/v3/rag/search?query=无人机空域申请")
check("GET /ai/v3/rag/search (URL中文)", code, 200, d)

code, d = http("GET", "/ai/v3/tools")
check("GET /ai/v3/tools", code, 200, d)

code, d = http("GET", "/drone/list?pageNum=1&pageSize=5")
check("GET /drone/list", code, 200, d)

# =============================================================
# 2) 鉴权：未登录应 401
# =============================================================
section("2. 鉴权：未登录")
code, d = http("POST", "/ai/v3/chat", body={"message": "test", "conversationId": "t"})
check("POST /ai/v3/chat 无 token", code, 401, d)

code, d = http("POST", "/airspace/submit", body={})
check("POST /airspace/submit 无 token", code, 401, d)

# =============================================================
# 3) 登录：管理员 + 普通用户
# =============================================================
section("3. 登录")
code, d = http("POST", "/auth/login", body={"username": "admin", "password": "123456"})
check("管理员登录 admin", code, 200, d)
admin_token = (d or {}).get("data", {}).get("token", "")
admin_id = (d or {}).get("data", {}).get("userId") or (d or {}).get("data", {}).get("user", {}).get("id")
print(f"   admin_id={admin_id}, token 长度={len(admin_token)}")

code, d = http("POST", "/auth/login", body={"username": "testuser", "password": "123456"})
check("普通用户登录 testuser", code, 200, d)
user_token = (d or {}).get("data", {}).get("token", "")
user_id = (d or {}).get("data", {}).get("userId") or (d or {}).get("data", {}).get("user", {}).get("id")
print(f"   user_id={user_id}, token 长度={len(user_token)}")

# =============================================================
# 4) 用户信息
# =============================================================
section("4. 用户信息")
code, d = http("GET", "/user/info", token=user_token)
check("GET /user/info (testuser)", code, 200, d)
if code == 200:
    print(f"   username={d['data'].get('username')}, role={d['data'].get('role')}, balance=¥{d['data'].get('balance')}")

code, d = http("GET", "/user/info", token=admin_token)
check("GET /user/info (admin)", code, 200, d)
if code == 200:
    print(f"   username={d['data'].get('username')}, role={d['data'].get('role')}, balance=¥{d['data'].get('balance')}")

# =============================================================
# 5) 无人机业务
# =============================================================
section("5. 无人机业务")
code, d = http("GET", "/drone/list?pageNum=1&pageSize=10", token=user_token)
check("GET /drone/list (鉴权)", code, 200, d)
if code == 200 and d.get("data", {}).get("records"):
    print(f"   共 {d['data']['total']} 架无人机")
    for r in d['data']['records'][:3]:
        print(f"   - {r.get('model')} ¥{r.get('pricePerDay')}/天 库存={r.get('stock')}")

# =============================================================
# 6) 空域提交
# =============================================================
section("6. 空域提交")
code, d = http("POST", "/airspace/submit", token=user_token, body={
    "regionName": "深圳市南山区测试空域",
    "latitude": 22.5333,
    "longitude": 113.9361,
    "maxAltitude": 120,
    "startTime": f"{date.today()} 08:00:00",
    "endTime": f"{(date.today() + timedelta(days=2))} 18:00:00",
    "purpose": "联调测试",
    "remark": "自动化测试"
})
check("POST /airspace/submit", code, 200, d)
if code == 200:
    print(f"   响应: {str(d)[:200]}")
airspace_id = None
if code == 200 and d.get("data"):
    airspace_id = d["data"].get("id") or (d["data"].get("record") or {}).get("id")
    print(f"   申请 ID: {airspace_id}")

# =============================================================
# 7) 资质状态查询
# =============================================================
section("7. 资质状态")
code, d = http("GET", "/user/qualification", token=user_token)
check("GET /user/qualification", code, 200, d)
if code == 200:
    print(f"   {d.get('data')}")

# =============================================================
# 8) 智能下单（带 RAG + Tool Calling）
# =============================================================
section("8. AI 智能客服对话 + 智能下单")
today = str(date.today())
end = str(date.today() + timedelta(days=2))
msg = f"用余额帮我下个单，DJI Air 2S，{today} 到 {end}，地址深圳市南山区，资质已通过，直接创建并支付"
code, d = http("POST", "/ai/v3/chat", token=user_token, body={
    "message": msg,
    "conversationId": "e2e-001"
})
check("POST /ai/v3/chat 智能下单", code, 200, d)
if code == 200:
    reply = d.get("data", {}).get("reply", "")
    print(f"   AI 回复(200字):\n   {reply[:300]}")
    m = re.search(r"ORD[A-Za-z0-9]+", reply)
    if m:
        order_no = m.group(0)
        print(f"\n   ➜ 提取订单号: {order_no}")
        # 落库核验
        conn = sqlite3.connect(DB); cur = conn.cursor()
        cur.execute("SELECT order_no, order_status, total_amount FROM rental_order WHERE order_no=?", (order_no,))
        row = cur.fetchone()
        if row:
            print(f"   ➜ DB 核验: order_no={row[0]}, status={row[1]}, ¥{row[2]}")
        conn.close()

# =============================================================
# 9) 多轮对话（Memory）
# =============================================================
section("9. 多轮对话记忆")
code, d = http("POST", "/ai/v3/chat", token=user_token, body={
    "message": "我上条消息问什么来着？",
    "conversationId": "e2e-001"
})
check("POST /ai/v3/chat 第2轮", code, 200, d)
if code == 200:
    print(f"   AI 回复: {d.get('data', {}).get('reply', '')[:150]}")

# =============================================================
# 10) RAG 知识库检索（带 RAG 引用）
# =============================================================
section("10. RAG + LLM 联动")
code, d = http("POST", "/ai/v3/chat", token=user_token, body={
    "message": "无人机保险怎么买？需要哪些材料？",
    "conversationId": "rag-001"
})
check("POST /ai/v3/chat 保险咨询", code, 200, d)
if code == 200:
    reply = d.get("data", {}).get("reply", "")
    print(f"   AI 回复: {reply[:300]}")

# =============================================================
# 11) 评论
# =============================================================
section("11. 评论")
code, d = http("GET", "/drone/2/comments?pageNum=1&pageSize=5", token=user_token)
check("GET /drone/2/comments", code, 200, d)
if code == 200:
    print(f"   评论数: {d.get('data', {}).get('total', 0)}")

# =============================================================
# 12) 通知
# =============================================================
section("12. 通知")
code, d = http("GET", "/notification/list", token=user_token)
check("GET /notification/list", code, 200, d)
if code == 200:
    n = d.get("data", {}).get("total", 0)
    print(f"   通知数: {n}")

code, d = http("GET", "/notification/unread-count", token=user_token)
check("GET /notification/unread-count", code, 200, d)
if code == 200:
    print(f"   未读数: {d.get('data')}")

# =============================================================
# 13) 订单 + 公共统计
# =============================================================
section("13. 订单 + 公共统计")
code, d = http("GET", "/user/orders?pageNum=1&pageSize=5", token=user_token)
check("GET /user/orders", code, 200, d)
if code == 200:
    print(f"   我的订单数: {d.get('data', {}).get('total', 0)}")

code, d = http("GET", "/user/order-stats", token=user_token)
check("GET /user/order-stats", code, 200, d)
if code == 200:
    print(f"   订单统计: {d.get('data')}")

code, d = http("GET", "/public/stats")
check("GET /public/stats", code, 200, d)
if code == 200:
    print(f"   {d.get('data')}")

# =============================================================
# 14) 管理员后台接口
# =============================================================
section("14. 管理员后台")
code, d = http("GET", "/admin/order/list?pageNum=1&pageSize=5", token=admin_token)
check("GET /admin/order/list", code, 200, d)
if code == 200:
    print(f"   订单总数: {d.get('data', {}).get('total', 0)}")

code, d = http("GET", "/admin/user/list?pageNum=1&pageSize=5", token=admin_token)
check("GET /admin/user/list", code, 200, d)
if code == 200:
    print(f"   用户总数: {d.get('data', {}).get('total', 0)}")

code, d = http("GET", "/admin/dashboard/stats", token=admin_token)
check("GET /admin/dashboard/stats", code, 200, d)
if code == 200:
    print(f"   {d.get('data')}")

code, d = http("GET", "/admin/dashboard/popular-drones", token=admin_token)
check("GET /admin/dashboard/popular-drones", code, 200, d)
if code == 200:
    print(f"   热门机型: {len(d.get('data', []))} 款")

code, d = http("GET", "/admin/user/qualification/list?pageNum=1&pageSize=5", token=admin_token)
check("GET /admin/user/qualification/list", code, 200, d)
if code == 200:
    print(f"   资质待审: {d.get('data', {}).get('total', 0)}")

# =============================================================
# 15) SSE 流式
# =============================================================
section("15. SSE 流式对话")
import socket
# 简单测一下连接 + 收头
try:
    s = socket.create_connection(("localhost", 8080), timeout=10)
    s.sendall(f"POST /api/ai/v3/chat/stream HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nAuthorization: Bearer {user_token}\r\nContent-Length: {len(json.dumps({'message':'hi','conversationId':'sse-001'}))}\r\n\r\n{json.dumps({'message':'hi','conversationId':'sse-001'})}".encode())
    s.settimeout(8)
    data = b""
    try:
        while b"\r\n\r\n" not in data:
            chunk = s.recv(1024)
            if not chunk: break
            data += chunk
    except: pass
    s.close()
    header = data.decode("utf-8", errors="ignore").split("\r\n")[0]
    print(f"   SSE 响应头: {header}")
    if "200" in header:
        check("POST /ai/v3/chat/stream SSE", 200, 200)
    else:
        check("POST /ai/v3/chat/stream SSE", 0, 200, header)
except Exception as e:
    check("POST /ai/v3/chat/stream SSE", 0, 200, str(e))

# =============================================================
# 15) MCP Server
# =============================================================
section("15. MCP Server SSE")
try:
    s = socket.create_connection(("localhost", 8080), timeout=5)
    s.sendall(f"GET /api/mcp/sse HTTP/1.1\r\nHost: localhost\r\nAccept: text/event-stream\r\nOrigin: http://localhost:3000\r\n\r\n".encode())
    s.settimeout(5)
    data = b""
    try:
        while b"\r\n\r\n" not in data:
            chunk = s.recv(1024)
            if not chunk: break
            data += chunk
    except: pass
    s.close()
    decoded = data.decode("utf-8", errors="ignore")
    first_line = decoded.split("\r\n")[0]
    print(f"   MCP 响应头: {first_line}")
    if "200" in first_line:
        check("GET /mcp/sse", 200, 200)
    else:
        check("GET /mcp/sse", 0, 200, decoded[:200])
except Exception as e:
    check("GET /mcp/sse", 0, 200, str(e))

# =============================================================
# 汇总
# =============================================================
print()
print("=" * 70)
print(f"【联调汇总】")
print(f"  通过: {len(passes)}")
print(f"  失败: {len(fails)}")
if fails:
    print("  失败项:")
    for f in fails:
        print(f"    - {f}")
print("=" * 70)
sys.exit(0 if not fails else 1)
