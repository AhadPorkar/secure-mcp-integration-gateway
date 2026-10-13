from pathlib import Path
import base64
import hashlib
import hmac
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SECRET = '0123456789abcdef0123456789abcdef'
PORT = '18096'
BASE = 'http://127.0.0.1:' + PORT


def tok(sub, tenant, scope, roles, aud='secure-mcp-gateway', ttl=600):
    enc = lambda b: base64.urlsafe_b64encode(b).decode().rstrip('=')
    header = enc(json.dumps({'alg': 'HS256', 'typ': 'JWT'}, separators=(',', ':')).encode())
    payload = enc(json.dumps({
        'iss': 'https://demo.identity.local',
        'aud': aud,
        'sub': sub,
        'tenant': tenant,
        'scope': scope,
        'roles': roles,
        'exp': int(time.time()) + ttl,
    }, separators=(',', ':')).encode())
    sig = enc(hmac.new(SECRET.encode(), f'{header}.{payload}'.encode(), hashlib.sha256).digest())
    return f'{header}.{payload}.{sig}'


def http_full(method, path, body=None, token=None, headers=None):
    data = None if body is None else json.dumps(body, separators=(',', ':')).encode()
    h = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    if token:
        h['Authorization'] = 'Bearer ' + token
    if headers:
        h.update(headers)
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            raw = r.read().decode()
            parsed = json.loads(raw) if raw and 'application/json' in r.headers.get('content-type', '') else raw
            return r.status, parsed, dict(r.headers.items())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            parsed = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            parsed = raw
        return e.code, parsed, dict(e.headers.items())


def http(method, path, body=None, token=None, headers=None):
    status, parsed, _ = http_full(method, path, body, token, headers)
    return status, parsed


def rpc(rid, method, params, token=None, name=None, proto='2026-07-28', meta_proto=None, header_method=None):
    p = dict(params)
    p['_meta'] = {
        'io.modelcontextprotocol/protocolVersion': meta_proto or proto,
        'io.modelcontextprotocol/clientCapabilities': {},
        'io.modelcontextprotocol/clientInfo': {'name': 'attack-matrix', 'version': '1.0'},
    }
    h = {'MCP-Protocol-Version': proto, 'Mcp-Method': header_method or method}
    if name:
        h['Mcp-Name'] = name
    return http('POST', '/mcp', {'jsonrpc': '2.0', 'id': rid, 'method': method, 'params': p}, token, h)


def check(cond, name, detail=''):
    if not cond:
        raise AssertionError(name + (': ' + detail if detail else ''))
    print('PASS', name)
    return 1


operator = tok('operator-acme', 'acme', 'orders:read inventory:read documents:read orders:cancel', ['operator'])
reviewer = tok('reviewer-acme', 'acme', '', ['approver'])
beta = tok('operator-beta', 'beta', 'orders:read inventory:read documents:read orders:cancel', ['operator'])
wrong_aud = tok('operator-acme', 'acme', 'orders:read', ['operator'], aud='other-api')
limited = tok('limited', 'acme', 'orders:read', ['operator'])
expired = tok('expired', 'acme', 'orders:read', ['operator'], ttl=-1)
combo = tok('combo', 'acme', 'orders:cancel', ['operator', 'approver'])
parts = operator.split('.')
sig_raw = bytearray(base64.urlsafe_b64decode(parts[2] + '=' * (-len(parts[2]) % 4)))
sig_raw[0] ^= 0x01
tampered_sig = base64.urlsafe_b64encode(bytes(sig_raw)).decode().rstrip('=')
tampered = f'{parts[0]}.{parts[1]}.{tampered_sig}'

env = os.environ.copy()
env['MCP_DEMO_HS256_SECRET'] = SECRET
env['MCP_GATEWAY_PORT'] = PORT
env['MCP_RESOURCE_URL'] = BASE + '/mcp'
p = subprocess.Popen(
    ['java', '-cp', str(ROOT / 'build/classes'), 'io.ahad.mcpgateway.Main'],
    cwd=ROOT,
    env=env,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
)

try:
    for _ in range(60):
        try:
            if http('GET', '/health')[0] == 200:
                break
        except Exception:
            pass
        time.sleep(.1)
    else:
        raise RuntimeError('server did not start: ' + (p.stdout.read() if p.stdout else ''))

    n = 0

    st, body = rpc(1, 'server/discover', {})
    n += check(st == 200 and '2026-07-28' in body['result']['supportedVersions'], 'mcp.discover')
    discover = body['result']
    n += check(
        discover['resultType'] == 'complete' and discover['cacheScope'] == 'public' and isinstance(discover['ttlMs'], int),
        'mcp.discover-result-shape')

    st, prm, headers = http_full('GET', '/.well-known/oauth-protected-resource/mcp')
    n += check(
        st == 200 and prm['resource'] == BASE + '/mcp' and prm['authorization_servers'] == ['https://demo.identity.local'],
        'oauth.protected-resource-metadata')
    n += check(headers.get('Access-control-allow-origin') == '*' or headers.get('Access-Control-Allow-Origin') == '*', 'oauth.metadata-cors')

    st, body = rpc(2, 'tools/list', {}, operator)
    n += check(st == 200 and len(body['result']['tools']) == 4, 'auth.scoped-tool-list')
    tool_list = body['result']
    n += check(
        tool_list['resultType'] == 'complete' and tool_list['cacheScope'] == 'private' and tool_list['ttlMs'] == 15000,
        'mcp.tool-list-result-shape')
    n += check(all(t['inputSchema'].get('additionalProperties') is False for t in tool_list['tools']), 'mcp.tool-schemas-closed')

    st, body, headers = http_full(
        'POST', '/mcp',
        {'jsonrpc': '2.0', 'id': 20, 'method': 'tools/list', 'params': {
            '_meta': {
                'io.modelcontextprotocol/protocolVersion': '2026-07-28',
                'io.modelcontextprotocol/clientCapabilities': {},
            }
        }},
        headers={'MCP-Protocol-Version': '2026-07-28', 'Mcp-Method': 'tools/list'})
    n += check(st == 401 and body['error']['message'] == 'missing_bearer_token', 'auth.missing-token')
    challenge = headers.get('Www-authenticate', headers.get('WWW-Authenticate', ''))
    n += check('resource_metadata=' in challenge and '/.well-known/oauth-protected-resource/mcp' in challenge, 'auth.challenge-resource-metadata')

    st, body = rpc(3, 'tools/list', {}, wrong_aud)
    n += check(st == 401 and body['error']['message'] == 'invalid_audience', 'auth.audience-rejected')
    st, body = rpc(30, 'tools/list', {}, expired)
    n += check(st == 401 and body['error']['message'] == 'token_expired', 'auth.expired-rejected')
    st, body = rpc(32, 'tools/list', {}, tampered)
    n += check(st == 401 and body['error']['message'] in {'invalid_signature', 'invalid_token'}, 'auth.tampered-token-rejected')

    st, body = rpc(31, 'tools/list', {}, limited)
    n += check(st == 200 and len(body['result']['tools']) == 1 and body['result']['tools'][0]['name'] == 'orders.lookup', 'auth.tool-list-least-privilege')
    st, body = rpc(4, 'tools/call', {'name': 'inventory.check', 'arguments': {'sku': 'SKU-RED'}}, limited, 'inventory.check')
    n += check(st == 403 and body['error']['message'] == 'insufficient_scope', 'auth.missing-scope')
    st, body = http('GET', '/admin/api/approvals', token=operator)
    n += check(st == 403 and body['error'] == 'approver_role_required', 'auth.admin-role-required')

    st, body = rpc(5, 'tools/call', {'name': 'orders.lookup', 'arguments': {'orderId': 'ORD-B200'}}, operator, 'orders.lookup')
    n += check(st == 404 and body['error']['message'] == 'not_found', 'tenant.cross-tenant-hidden')
    st, body = rpc(33, 'tools/call', {'name': 'orders.lookup', 'arguments': {'orderId': 'ORD-A100', 'tenant': 'beta'}}, operator, 'orders.lookup')
    n += check(st == 400 and body['error']['message'].startswith('unknown_argument'), 'tenant.argument-injection-rejected')

    st, body = rpc(6, 'tools/call', {'name': 'orders.lookup', 'arguments': {'orderId': 'ORD-A100'}}, operator, 'inventory.check')
    n += check(st == 400 and 'header_mismatch' in body['error']['message'], 'mcp.header-name-binding')
    st, body = rpc(34, 'tools/list', {}, operator, header_method='tools/call')
    n += check(st == 400 and body['error']['message'] == 'header_mismatch:mcp-method', 'mcp.header-method-binding')
    st, body = rpc(35, 'tools/list', {}, operator, proto='2025-11-25')
    n += check(st == 400 and body['error']['message'] == 'unsupported_protocol_version', 'mcp.protocol-version-rejected')
    st, body = rpc(36, 'tools/list', {}, operator, meta_proto='2025-11-25')
    n += check(st == 400 and body['error']['message'] == 'meta_protocol_mismatch', 'mcp.meta-protocol-binding')
    st, body = rpc(37, 'prompts/list', {}, operator)
    n += check(st == 404 and body['error']['message'] == 'method_not_found', 'mcp.unimplemented-method-fails-closed')

    st, body = rpc(7, 'tools/call', {'name': 'sql.execute', 'arguments': {'sql': 'DROP TABLE x'}}, operator, 'sql.execute')
    n += check(st == 404 and body['error']['message'] == 'unknown_tool', 'attack.arbitrary-sql-not-exposed')
    st, body = rpc(8, 'tools/call', {'name': 'orders.lookup', 'arguments': {'orderId': 'ORD-A100', 'url': 'https://evil.invalid'}}, operator, 'orders.lookup')
    n += check(st == 400 and body['error']['message'].startswith('unknown_argument'), 'attack.destination-argument-rejected')
    st, body = rpc(38, 'tools/call', {'name': 'orders.lookup', 'arguments': {'orderId': "ORD-A100';DROP"}}, operator, 'orders.lookup')
    n += check(st == 400 and body['error']['message'] == 'invalid_argument:orderId', 'attack.identifier-injection-rejected')

    st, body = rpc(9, 'tools/call', {'name': 'orders.cancel', 'arguments': {'orderId': 'ORD-A100', 'reason': 'Customer request'}}, operator, 'orders.cancel')
    approval_id = body['result']['structuredContent']['approvalId']
    n += check(st == 200 and body['result']['structuredContent']['approvalRequired'] is True, 'approval.request-created')

    st, body = http('POST', f'/admin/api/approvals/{approval_id}/approve', {'reason': 'wrong tenant'}, beta)
    n += check(st == 403, 'approval.cross-tenant-review-hidden')
    st, body = rpc(40, 'tools/call', {'name': 'orders.cancel', 'arguments': {'orderId': 'ORD-A100', 'reason': 'Customer request', 'approvalId': approval_id}}, beta, 'orders.cancel')
    n += check(st == 403 and body['error']['message'] == 'approval_not_found', 'approval.cross-tenant-execution-blocked')

    st, body = rpc(10, 'tools/call', {'name': 'orders.cancel', 'arguments': {'orderId': 'ORD-A100', 'reason': 'Customer request'}}, combo, 'orders.cancel')
    self_id = body['result']['structuredContent']['approvalId']
    st, body = http('POST', f'/admin/api/approvals/{self_id}/approve', {'reason': 'self'}, combo)
    n += check(st == 403 and body['error'] == 'self_approval_forbidden', 'approval.self-approval-blocked')

    st, body = http('POST', f'/admin/api/approvals/{approval_id}/approve', {'reason': 'Verified independently'}, reviewer)
    n += check(st == 200 and body['status'] == 'APPROVED', 'approval.independent-review')
    st, body = rpc(11, 'tools/call', {'name': 'orders.cancel', 'arguments': {'orderId': 'ORD-A100', 'reason': 'Tampered reason', 'approvalId': approval_id}}, operator, 'orders.cancel')
    n += check(st == 403 and body['error']['message'] == 'approval_binding_mismatch', 'approval.argument-tamper-blocked')

    exact = {'name': 'orders.cancel', 'arguments': {'orderId': 'ORD-A100', 'reason': 'Customer request', 'approvalId': approval_id}}
    st, body = rpc(12, 'tools/call', exact, operator, 'orders.cancel')
    receipt1 = body['result']['structuredContent']
    n += check(st == 200 and receipt1['status'] == 'CANCELLED', 'write.approved-execution')
    n += check(body['result']['resultType'] == 'complete' and body['result']['isError'] is False, 'mcp.call-result-shape')
    st, body = rpc(13, 'tools/call', exact, operator, 'orders.cancel')
    receipt2 = body['result']['structuredContent']
    n += check(st == 200 and receipt1['receiptId'] == receipt2['receiptId'], 'write.idempotent-retry')

    st, body = rpc(14, 'tools/call', {'name': 'customer.documents.list', 'arguments': {'customerId': 'CUS-B2'}}, operator, 'customer.documents.list')
    n += check(st == 404, 'documents.cross-tenant-hidden')

    st, body = http('GET', '/admin/api/audit', token=reviewer)
    n += check(st == 200 and body['chainValid'] is True and len(body['items']) >= 5, 'audit.chain-verifies')
    n += check('Customer request' not in json.dumps(body), 'audit.no-raw-business-arguments')

    req = urllib.request.Request(BASE + '/admin', method='GET')
    with urllib.request.urlopen(req, timeout=5) as rr:
        csp = rr.headers.get('Content-Security-Policy', '')
    n += check("'unsafe-inline'" not in csp and "'unsafe-eval'" not in csp and "script-src 'self'" in csp, 'browser.strict-csp')
    st, js, _ = http_full('GET', '/admin/assets/app.js')
    n += check(st == 200 and 'innerHTML' not in js and 'localStorage' not in js and 'sessionStorage' not in js, 'browser.safe-dom-and-memory-token')

    quota = tok('quota-user', 'acme', 'inventory:read', ['operator'])
    last = None
    for i in range(31):
        last = rpc(100 + i, 'tools/call', {'name': 'inventory.check', 'arguments': {'sku': 'SKU-RED'}}, quota, 'inventory.check')
    n += check(last[0] == 429 and last[1]['error']['message'] == 'quota_exceeded', 'quota.enforced')

    print('ATTACK_MATRIX_PASSED=' + str(n))
finally:
    p.terminate()
    try:
        p.wait(timeout=3)
    except subprocess.TimeoutExpired:
        p.kill()
