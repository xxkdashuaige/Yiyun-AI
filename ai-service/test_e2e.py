import requests
import time

BASE = 'http://localhost:8080'

# ============ 1. 登录 ============
print("=" * 60)
print("1. 登录测试")
r = requests.post(f'{BASE}/api/auth/login', json={'username': 'admin', 'password': '123456'})
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}")
assert data.get('code') == 200, "登录失败"
token = data['data']['token']
print(f"   Token 获取成功: {token[:40]}...")
headers = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json'}

# ============ 2. 会话列表 ============
print("\n" + "=" * 60)
print("2. 会话列表测试")
r = requests.get(f'{BASE}/api/ai/sessions', headers=headers)
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}, 会话数: {len(data.get('data', []))}")

# ============ 3. 非遗项目列表 ============
print("\n" + "=" * 60)
print("3. 非遗项目列表测试")
r = requests.get(f'{BASE}/api/heritage/projects?pageNum=1&pageSize=4')
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}")
records = data.get('data', {}).get('records', []) if data.get('data') else []
print(f"   项目数: {len(records)}")
project_id = records[0]['id'] if records else None
print(f"   第一个项目ID: {project_id}, 名称: {records[0]['name'] if records else 'N/A'}")

# ============ 4. AI 同步对话 ============
print("\n" + "=" * 60)
print("4. AI 同步对话测试")
start = time.time()
r = requests.post(f'{BASE}/api/ai/chat', json={'message': '什么是非遗？'}, headers=headers, timeout=120)
elapsed = time.time() - start
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}")
print(f"   耗时: {elapsed:.1f}s")
chat_data = data.get('data', {})
session_id = chat_data.get('sessionId')
print(f"   sessionId: {session_id}")
print(f"   reply长度: {len(chat_data.get('reply', ''))}")
print(f"   sources数量: {len(chat_data.get('sources', []))}")
assert data.get('code') == 200, "对话失败"
assert session_id is not None, "未返回 sessionId"

# ============ 5. 获取会话消息 ============
print("\n" + "=" * 60)
print("5. 获取会话消息测试")
r = requests.get(f'{BASE}/api/ai/sessions/{session_id}/messages', headers=headers)
data = r.json()
msgs = data.get('data', [])
print(f"   状态码: {r.status_code}, 消息数: {len(msgs)}")
for m in msgs:
    print(f"     role={m.get('role')}, content_len={len(m.get('content', ''))}")

# ============ 6. AI 流式对话 ============
print("\n" + "=" * 60)
print("6. AI 流式对话测试")
r = requests.post(f'{BASE}/api/ai/chat/stream', json={'message': '介绍一下昆曲', 'sessionId': session_id},
                  headers=headers, stream=True, timeout=120)
print(f"   状态码: {r.status_code}, Content-Type: {r.headers.get('content-type')}")
delta_count = 0
sources_received = False
done_received = False
for line in r.iter_lines(decode_unicode=True):
    if not line:
        continue
    if not line.startswith('data:'):
        continue
    payload = line[5:].strip()
    if payload == '[DONE]':
        done_received = True
        break
    try:
        import json
        obj = json.loads(payload)
        if 'delta' in obj:
            delta_count += 1
        elif 'sources' in obj:
            sources_received = True
            print(f"   sources数量: {len(obj['sources'])}, session_id: {obj.get('session_id')}")
        elif 'error' in obj:
            print(f"   错误: {obj['error']}")
    except:
        pass
print(f"   delta片段数: {delta_count}, 收到sources: {sources_received}, 收到DONE: {done_received}")
assert delta_count > 0, "未收到 delta 片段"
assert done_received, "未收到 [DONE]"

# ============ 7. 多轮对话验证 ============
print("\n" + "=" * 60)
print("7. 多轮对话验证（同一 sessionId）")
r = requests.post(f'{BASE}/api/ai/chat', json={'message': '它有什么特点？', 'sessionId': session_id}, headers=headers, timeout=120)
data = r.json()
new_session_id = data.get('data', {}).get('sessionId')
print(f"   新sessionId: {new_session_id}, 与原session一致: {new_session_id == session_id}")
assert new_session_id == session_id, "多轮对话 sessionId 不一致"

# ============ 8. 删除会话 ============
print("\n" + "=" * 60)
print("8. 删除会话测试")
r = requests.delete(f'{BASE}/api/ai/sessions/{session_id}', headers=headers)
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}")
r = requests.get(f'{BASE}/api/ai/sessions/{session_id}/messages', headers=headers)
print(f"   删除后查询消息状态码: {r.status_code} (期望 404 或 500)")

# ============ 9. 生图功能 ============
print("\n" + "=" * 60)
print("9. 生图功能测试")
r = requests.post(f'{BASE}/api/ai/generate/image',
                  json={'prompt': '一只可爱的猫', 'style': '国画', 'size': '1:1', 'count': 1},
                  headers=headers, timeout=30)
data = r.json()
print(f"   状态码: {r.status_code}, code: {data.get('code')}")
print(f"   响应: {str(data)[:300]}")

print("\n" + "=" * 60)
print("全部测试完成！")