#!/usr/bin/env python3
import socket, ssl, hashlib, base64, time, threading, sys, json, os

if len(sys.argv) < 2:
    print("用法: python3 ws-test2.py <token>")
    sys.exit(1)

token = sys.argv[1]

host, port = "localhost", 8080
path = f"/api/ws/orders?token={token}"

key = base64.b64encode(os.urandom(16)).decode()

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(10)
sock.connect((host, port))

upgrade_req = (
    f"GET {path} HTTP/1.1\r\n"
    f"Host: {host}:{port}\r\n"
    "Upgrade: websocket\r\n"
    "Connection: Upgrade\r\n"
    f"Sec-WebSocket-Key: {key}\r\n"
    "Sec-WebSocket-Version: 13\r\n"
    "User-Agent: test-client\r\n"
    "\r\n"
)

sock.sendall(upgrade_req.encode())

print(f"[ws] 请求: GET {path}")

buffer = b""
while b"\r\n\r\n" not in buffer:
    chunk = sock.recv(2048)
    if not chunk:
        break
    buffer += chunk

header_part, _, body = buffer.partition(b"\r\n\r\n")
print(f"[ws] 握手响应头:")
for line in header_part.decode().split("\r\n"):
    print(f"  {line}")

status_line = header_part.decode().split("\r\n")[0]
if "101" not in status_line:
    print(f"\n[ws] 握手失败，服务器返回非 101")
    print(f"[ws] 响应体: {body.decode()}")
    sys.exit(1)

print("\n[ws] WebSocket 升级成功，开始监听消息（15 秒超时）")
print("[ws] 请在另一个终端创建订单，消息会自动显示")
print("-" * 60)

def recv_frame(s):
    try:
        header = s.recv(2)
        if len(header) < 2:
            return None
        b1, b2 = header[0], header[1]
        opcode = b1 & 0x0F
        payload_len = b2 & 0x7F

        if payload_len == 126:
            ext = s.recv(2)
            payload_len = int.from_bytes(ext, "big")
        elif payload_len == 127:
            ext = s.recv(8)
            payload_len = int.from_bytes(ext, "big")

        payload = b""
        while len(payload) < payload_len:
            chunk = s.recv(payload_len - len(payload))
            if not chunk:
                break
            payload += chunk

        return opcode, payload.decode("utf-8", errors="replace")
    except socket.timeout:
        return 99, "timeout"
    except Exception as e:
        return 98, str(e)

start = time.time()
while time.time() - start < 15:
    result = recv_frame(sock)
    if result is None:
        print("[ws] 连接已关闭")
        break
    opcode, data = result
    if opcode == 99:
        continue
    if opcode == 98:
        print(f"[ws] 错误: {data}")
        break
    if opcode == 8:
        print(f"[ws] 服务端关闭: {data}")
        break
    if opcode == 1:
        try:
            obj = json.loads(data)
            print(f"\n[ws] 收到消息 (JSON):")
            print(json.dumps(obj, ensure_ascii=False, indent=2))
        except:
            print(f"\n[ws] 收到消息: {data}")
    else:
        print(f"[ws] opcode={opcode}: {data}")

print("\n测试完成")
sock.close()
