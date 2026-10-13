from pathlib import Path
import base64
import hashlib
import hmac
import json
import os
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from playwright.sync_api import expect, sync_playwright

ROOT = Path(__file__).resolve().parents[1]
PORT = 18097
BASE = f'http://127.0.0.1:{PORT}'
BROWSER_ORIGIN = 'https://mcp.test'
SECRET = '0123456789abcdef0123456789abcdef'


def token(sub, tenant, scope, roles, ttl=600):
    enc = lambda b: base64.urlsafe_b64encode(b).decode().rstrip('=')
    h = enc(json.dumps({'alg': 'HS256', 'typ': 'JWT'}, separators=(',', ':')).encode())
    p = enc(json.dumps({
        'iss': 'https://demo.identity.local',
        'aud': 'secure-mcp-gateway',
        'sub': sub,
        'tenant': tenant,
        'scope': scope,
        'roles': roles,
        'exp': int(time.time()) + ttl,
    }, separators=(',', ':')).encode())
    s = enc(hmac.new(SECRET.encode(), f'{h}.{p}'.encode(), hashlib.sha256).digest())
    return f'{h}.{p}.{s}'


def raw_request(method, path, body=None, headers=None):
    data = None if body is None else body if isinstance(body, bytes) else body.encode()
    h = dict(headers or {})
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            return r.status, r.read(), dict(r.headers.items())
    except urllib.error.HTTPError as e:
        return e.code, e.read(), dict(e.headers.items())


def request_json(method, path, body=None, bearer=None, headers=None):
    h = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    if bearer:
        h['Authorization'] = 'Bearer ' + bearer
    if headers:
        h.update(headers)
    payload = None if body is None else json.dumps(body, separators=(',', ':')).encode()
    status, raw, _ = raw_request(method, path, payload, h)
    return status, json.loads(raw.decode())


def rpc(rid, name, arguments, bearer):
    meta = {
        'io.modelcontextprotocol/protocolVersion': '2026-07-28',
        'io.modelcontextprotocol/clientCapabilities': {},
        'io.modelcontextprotocol/clientInfo': {'name': 'browser-test', 'version': '1.0'},
    }
    return request_json(
        'POST', '/mcp',
        {'jsonrpc': '2.0', 'id': rid, 'method': 'tools/call', 'params': {'name': name, 'arguments': arguments, '_meta': meta}},
        bearer,
        {'MCP-Protocol-Version': '2026-07-28', 'Mcp-Method': 'tools/call', 'Mcp-Name': name},
    )


def main():
    operator = token('browser-operator', 'acme', 'orders:cancel', ['operator'])
    reviewer = token('browser-reviewer', 'acme', '', ['approver'])
    env = os.environ.copy()
    env['MCP_DEMO_HS256_SECRET'] = SECRET
    env['MCP_GATEWAY_PORT'] = str(PORT)
    env['MCP_RESOURCE_URL'] = BASE + '/mcp'
    proc = subprocess.Popen(
        ['java', '-cp', str(ROOT / 'build/classes'), 'io.ahad.mcpgateway.Main'],
        cwd=ROOT,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    try:
        for _ in range(80):
            try:
                if request_json('GET', '/health')[0] == 200:
                    break
            except Exception:
                pass
            time.sleep(.1)
        else:
            raise RuntimeError('browser test server did not start')

        status, response = rpc(1, 'orders.cancel', {'orderId': 'ORD-A100', 'reason': 'Customer request'}, operator)
        assert status == 200
        approval_id = response['result']['structuredContent']['approvalId']

        with sync_playwright() as p:
            system_chromium = os.environ.get('PLAYWRIGHT_CHROMIUM_EXECUTABLE')
            if not system_chromium and Path('/usr/bin/chromium').exists():
                system_chromium = '/usr/bin/chromium'
            launch_args = {'headless': True}
            if system_chromium:
                launch_args['executable_path'] = system_chromium
            browser = p.chromium.launch(**launch_args)
            page = browser.new_page(viewport={'width': 1440, 'height': 1000})

            def proxy(route):
                parsed = urllib.parse.urlsplit(route.request.url)
                path = parsed.path + (('?' + parsed.query) if parsed.query else '')
                incoming = route.request.headers
                forward_headers = {}
                for key in ('authorization', 'content-type', 'accept'):
                    if key in incoming:
                        forward_headers[key] = incoming[key]
                data = route.request.post_data_buffer
                status_code, payload, response_headers = raw_request(route.request.method, path, data, forward_headers)
                allowed = {}
                for key, value in response_headers.items():
                    if key.lower() in {'content-type', 'content-security-policy', 'x-content-type-options', 'x-frame-options', 'referrer-policy', 'cache-control'}:
                        allowed[key] = value
                route.fulfill(status=status_code, headers=allowed, body=payload)

            page.route(BROWSER_ORIGIN + '/**', proxy)
            response_page = page.goto(BROWSER_ORIGIN + '/admin', wait_until='networkidle')
            assert response_page is not None and response_page.ok
            csp = response_page.headers.get('content-security-policy', '')
            assert "script-src 'self'" in csp and "'unsafe-inline'" not in csp and "'unsafe-eval'" not in csp

            expect(page.locator('h1')).to_have_text('Secure MCP Integration Gateway')
            page.locator('#token').fill(reviewer)
            page.locator('#load').click()
            expect(page.locator('#status')).to_have_text('Loaded', timeout=10000)
            expect(page.locator('#approvals')).to_contain_text(approval_id[:8])
            expect(page.locator(f'button[data-approve="{approval_id}"]')).to_be_visible()

            malicious_reason = '</pre><img id="xss-probe" src=x onerror="window.__xss=1">'
            page.once('dialog', lambda dialog: dialog.accept(malicious_reason))
            page.locator(f'button[data-approve="{approval_id}"]').click()
            expect(page.locator('#status')).to_have_text('Loaded', timeout=10000)
            expect(page.locator('#approvals')).to_contain_text('APPROVED', timeout=10000)
            expect(page.locator('#audit')).to_contain_text('approval.approve', timeout=10000)
            assert page.locator('#xss-probe').count() == 0
            assert page.evaluate('() => window.__xss === undefined') is True

            page.reload(wait_until='networkidle')
            expect(page.locator('#token')).to_have_value('')
            assert page.evaluate('() => localStorage.length') == 0
            assert page.evaluate('() => sessionStorage.length') == 0
            browser.close()

        print('BROWSER_TESTS_PASSED=10')
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()


if __name__ == '__main__':
    main()
