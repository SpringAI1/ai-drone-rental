#!/usr/bin/env python3
"""硬核实测：AI 智能下单 + 数据库验证"""
import json
import sqlite3
import re
import urllib.request
import urllib.error
from datetime import date, timedelta

BASE = "http://localhost:8080/api"
DB = "/Users/chenxi/Desktop/AI-Rental/backend/drone_rental.db"


def http(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body_text = e.read().decode("utf-8") or "{}"
        try:
            return e.code, json.loads(body_text)
        except Exception:
            return e.code, {"raw": body_text}


def section(title):
    print()
    print("=" * 60)
    print(title)
    print("=" * 60)


# 1) 登录
section("Step 1: 登录 testuser")
code, d = http("POST", "/auth/login", body={"username": "testuser", "password": "123456"})
token = d["data"]["token"]
user_id = d["data"].get("userId") or d["data"].get("user", {}).get("id")
print(f"  HTTP {code}, userId={user_id}, token 长度={len(token)}")

# 2) 查询余额基线
section("Step 2: 查询当前余额（基线）")
code, d = http("GET", "/user/info", token=token)
balance_before = d["data"].get("balance", 0)
print(f"  HTTP {code}, 当前余额: ¥{balance_before}")

# 3) AI 智能下单
section("Step 3: 通过 AI 对话触发智能下单（@Tool function calling）")
today = str(date.today())
end = str(date.today() + timedelta(days=3))
msg = f"我要租无人机，机型 DJI Mavic 3，租期 {today} 到 {end}，共 3 天，地址是深圳市南山区科技园，资质已通过，用余额支付，请直接帮我创建订单"
print(f"  用户消息: {msg}")
code, d = http("POST", "/ai/v3/chat", token=token, body={
    "message": msg,
    "conversationId": "real-verify-001"
})
print(f"  HTTP {code}, 耗时: {d['data']['elapsedMs']} ms")
print(f"  AI 回复:")
print(f"  {d['data']['reply']}")
order_no_match = re.search(r"ORD[A-Za-z0-9]+", d["data"]["reply"])
order_no = order_no_match.group(0) if order_no_match else None
print(f"  ➜ 提取到订单号: {order_no}")

# 4) 查 SQLite 数据库
section(f"Step 4: 直接查 SQLite 数据库 ({DB})")
conn = sqlite3.connect(DB)
conn.row_factory = sqlite3.Row
cur = conn.cursor()

cur.execute("SELECT id, username, balance FROM user WHERE id=?", (user_id,))
user_row = cur.fetchone()
print(f"  user 表: id={user_row['id']}, name={user_row['username']}, balance=¥{user_row['balance']}")

if order_no:
    cur.execute("SELECT * FROM rental_order WHERE order_no=?", (order_no,))
    order_row = cur.fetchone()
    if order_row:
        print(f"\n  rental_order 表找到订单 {order_no}:")
        for k in order_row.keys():
            v = order_row[k]
            if isinstance(v, str) and len(v) > 100:
                v = v[:100] + "..."
            print(f"    {k}: {v}")
    else:
        print(f"  ❌ rental_order 表未找到 {order_no}")

    cur.execute("SELECT * FROM payment WHERE order_no=?", (order_no,))
    pay_row = cur.fetchone()
    if pay_row:
        print(f"\n  payment 表找到支付记录:")
        for k in pay_row.keys():
            print(f"    {k}: {pay_row[k]}")
    else:
        print(f"  ⚠️ payment 表无此订单支付记录")
else:
    print("  ⚠️ 未提取到订单号")
    order_row = None
    pay_row = None

# 用户历史订单数
cur.execute("SELECT COUNT(*) as cnt FROM rental_order WHERE user_id=?", (user_id,))
print(f"\n  ➜ 用户 {user_id} 历史订单总数: {cur.fetchone()['cnt']}")
conn.close()

# 5) 结论
section("Step 5: 验证结论")
if order_no and order_row:
    print(f"  ✅ 真实下单成功: {order_no}")
    print(f"     订单状态(order_status): {order_row['order_status']}（0=待支付,1=已支付,2=已发货...）")
    print(f"     总金额: ¥{order_row['total_amount']}")
    if pay_row:
        print(f"     支付单号: {pay_row['payment_no']}")
        print(f"     支付状态: {pay_row['payment_status']}（0=待支付,1=支付成功）")
    delta = balance_before - user_row["balance"]
    print(f"     余额变化: ¥{balance_before} → ¥{user_row['balance']} (差额 ¥{delta:.2f})")
    print()
    print("  ✅ AI 智能下单已实打实落到数据库，不是模拟！")
else:
    print("  ❌ 订单未在数据库中找到")
