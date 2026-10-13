# Configuration

## Gateway identity settings

```text
MCP_GATEWAY_PORT=8096
MCP_DEMO_HS256_SECRET=<32+ characters, demo only>
MCP_TOKEN_ISSUER=https://demo.identity.local
MCP_TOKEN_AUDIENCE=secure-mcp-gateway
MCP_AUTHORIZATION_SERVER=https://demo.identity.local
MCP_RESOURCE_URL=http://127.0.0.1:8096/mcp
MCP_APPROVAL_TTL_SECONDS=600
MCP_QUOTA_PER_MINUTE=30
```

The HMAC secret is only a runnable demo seam. Production must use trusted OAuth/OIDC access tokens and JWKS-backed validation.

## Connector Hub HTTP mode

When `CONNECTOR_HUB_BASE_URL` is absent, deterministic synthetic data is used. To enable the HTTP adapter, configure all fixed mappings:

```text
CONNECTOR_HUB_BASE_URL=https://connector.internal.example
CONNECTOR_HUB_SERVICE_KEY=<dedicated gateway credential>
CONNECTOR_HUB_CREDENTIAL_MODE=service-header
CONNECTOR_HUB_ALLOWED_HOSTS=connector.internal.example
CONNECTOR_HUB_ORDER_LOOKUP_PATH=/published/order-lookup/v1/invoke
CONNECTOR_HUB_INVENTORY_CHECK_PATH=/published/inventory-check/v1/invoke
CONNECTOR_HUB_DOCUMENTS_LIST_PATH=/published/customer-documents/v1/invoke
CONNECTOR_HUB_ORDER_CANCEL_PATH=/published/order-cancel/v1/invoke
```

`service-header` sends the dedicated key in `x-connector-service-key`. `bearer` sends that same dedicated service credential as a bearer token; it still never reuses the MCP caller token.

Non-loopback hosts require HTTPS and explicit allowlisting. Fixed paths reject scheme markers, parent traversal, query/fragment injection, user info, backslashes and CR/LF.
