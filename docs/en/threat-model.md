# Threat model

| Threat | Primary control | Executable evidence |
|---|---|---|
| Token minted for another API | issuer/audience/expiry validation | `auth.audience-rejected`, expired/tampered cases |
| Missing permission | scope-filtered list + call-time check | `auth.missing-scope`, least-privilege list |
| Cross-tenant access | identity-derived tenant | order/document/approval isolation cases |
| Arbitrary SQL | capability never published | `attack.arbitrary-sql-not-exposed` |
| Attacker-controlled destination / SSRF | no URL arg + fixed paths + HTTPS/allowlist | URL injection + Java URI/path contracts |
| Header/body confusion | MCP protocol/method/name binding | MCP mismatch scenarios |
| Token passthrough | dedicated Connector Hub credential | Java credential-isolation contracts |
| Self/cross-tenant approval | role + requester + tenant binding | approval denial scenarios |
| Approval reuse after modification | tool revision + canonical args hash | `approval.argument-tamper-blocked` |
| Duplicate business effect | stable idempotency key | retry + concurrent Java contracts |
| Audit alteration | SHA-256 chain | `audit.chain-verifies` |
| Sensitive data in audit | hash details, no raw args | `audit.no-raw-business-arguments` |
| Request flooding | per-principal/per-tool quota | `quota.enforced` |
| Reviewer token theft from browser storage | no persistent browser storage | static served-JS check + manual browser suite |

Residual risk remains around production identity integration, durable/distributed state, upstream provider correctness, endpoint hardening, egress infrastructure and operational key management.
