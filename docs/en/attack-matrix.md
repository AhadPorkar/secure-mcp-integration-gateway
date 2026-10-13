# Executable attack matrix

`tests/attack_matrix.py` starts the actual Java gateway process and attacks its HTTP/MCP boundary. The release-candidate run contains **40 passed live scenarios**, in addition to **26 Java contract assertions** and **11 repository integrity checks**.

| Category | Representative denial / invariant |
|---|---|
| Authentication | missing, expired, tampered, wrong-audience token |
| Authorization | least-privilege tool list, missing scope, admin role required |
| Tenant isolation | cross-tenant order/document/approval access hidden |
| MCP binding | protocol/meta/header/name mismatch rejected |
| Tool injection | arbitrary SQL absent, URL/destination field rejected, identifier injection rejected |
| Approval | self-approval, cross-tenant review, requester mismatch and argument tampering blocked |
| Write safety | exact approved execution + idempotent retry |
| Audit | hash chain verifies; raw business arguments not persisted |
| Browser boundary | strict CSP; served JS has no `innerHTML`, `localStorage` or `sessionStorage` token pattern |
| Abuse control | per-principal/per-tool quota enforced |

The exact names and console output are recorded in `docs/evidence/test-run.txt`.

## Separate release gates

The native Playwright reviewer-console test and official MCP TypeScript v2 client smoke test are included but not counted in the 77 locally verified checks. They have dedicated manual GitHub workflows so a constrained authoring environment cannot create false red/green evidence.
