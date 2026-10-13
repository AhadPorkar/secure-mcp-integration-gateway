# Konfiguration

Gateway-Demo:

```text
MCP_GATEWAY_PORT=8096
MCP_DEMO_HS256_SECRET=<32+ Zeichen, nur Demo>
MCP_TOKEN_ISSUER=https://demo.identity.local
MCP_TOKEN_AUDIENCE=secure-mcp-gateway
MCP_AUTHORIZATION_SERVER=https://demo.identity.local
MCP_RESOURCE_URL=http://127.0.0.1:8096/mcp
MCP_APPROVAL_TTL_SECONDS=600
MCP_QUOTA_PER_MINUTE=30
```

Connector-Hub-HTTP-Modus benötigt `CONNECTOR_HUB_BASE_URL`, ein dediziertes `CONNECTOR_HUB_SERVICE_KEY`, eine Host-Allowlist sowie vier feste relative Provider-Pfade. Externe Hosts muessen HTTPS verwenden. Pfade mit Scheme, `..`, Query/Fragment, Userinfo, Backslash oder CR/LF werden abgelehnt.

Der Caller-Bearer-Token wird niemals als Connector-Hub-Credential verwendet.
