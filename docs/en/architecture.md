# Architecture

The gateway is a separate security adapter between an MCP host and Connector Hub. It does not replace Connector Hub and does not expose Connector Hub credentials to the model-facing boundary.

```text
MCP host
  | bearer token
  v
Secure MCP Integration Gateway
  |- JWT verification -> subject / tenant / scopes / roles
  |- tool registry -> closed schemas + immutable revisions
  |- quota limiter
  |- approval store -> exact revision + canonical argument hash
  |- tamper-evident audit ledger
  v dedicated service credential
Connector Hub
  v
Published providers / enterprise systems
```

## Request path

1. The HTTP boundary validates the JSON-RPC envelope and the supported MCP `2026-07-28` protocol metadata.
2. Protected operations verify issuer, audience, expiry, subject and tenant.
3. `tools/list` exposes only tools whose required scopes are present and returns a `private` cache scope.
4. `tools/call` rejects unknown fields before dispatch. Tenant and destination are not tool arguments.
5. Quota enforcement happens before provider execution.
6. Read tools execute immediately through a tenant-aware Connector Hub client.
7. A write without `approvalId` creates a pending approval and performs no write.
8. An exact approved write is executed with a stable upstream idempotency key.
9. Security decisions are appended to a SHA-256-linked audit chain.

## State and trust zones

The release candidate intentionally keeps approvals, quotas and audit state in-process so the control flow is easy to review. That is a demo/reference trade-off, not a horizontal-scaling design. Production requires durable shared stores and transactionally safe execution semantics.

## Failure model

- Unknown tool, unknown argument, malformed identifier and missing scope fail closed.
- Cross-tenant identifiers are intentionally indistinguishable from absent data in the demo backend.
- If Connector Hub rejects or times out, the gateway does not silently switch destinations.
- Write retry safety depends on the mapped Connector Hub write provider honoring the supplied idempotency key.
