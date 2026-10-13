package io.ahad.mcpgateway;

public final class AdminPage {
    private AdminPage() {}
    public static String html(){ return """
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Secure MCP Gateway</title><link rel="stylesheet" href="/admin/assets/app.css"></head>
<body><div class="wrap"><div class="hero"><span class="tag">MCP 2026-07-28 subset</span><h1>Secure MCP Integration Gateway</h1><p class="muted">Human approval, tenant isolation, exact-argument binding, quotas and tamper-evident audit around Connector Hub tools.</p><div class="row"><input id="token" type="password" placeholder="Paste reviewer demo token (kept only in this page memory)"><button id="load">Load</button><span id="status"></span></div></div>
<div class="grid"><section class="card"><h2>Pending approvals</h2><div id="approvals" class="muted">Load with a reviewer token.</div></section><section class="card"><h2>Audit ledger</h2><div id="audit" class="muted">No data loaded.</div></section></div></div><script src="/admin/assets/app.js" defer></script></body></html>
"""; }
}
