# Threat Model

| Bedrohung | Control | Evidenz |
|---|---|---|
| Token für andere API | Audience/Issuer/Expiry | Auth-Angriffstests |
| Fehlende Berechtigung | Scope List + Call Check | Least Privilege / Missing Scope |
| Cross-Tenant | Identity-Tenant | Order/Document/Approval Isolation |
| Beliebiges SQL | Capability nicht publiziert | SQL-Negativtest |
| SSRF/Ziel-Injection | Kein URL-Arg + feste Pfade + HTTPS/Allowlist | URL-Test + Java Contracts |
| Header/Body Confusion | MCP Header-Bindung | MCP Mismatch Tests |
| Token Passthrough | separates Service Credential | Java Credential Contracts |
| Self/Cross-Tenant Approval | Role/Requester/Tenant Binding | Approval Tests |
| Approval Tampering | Revision + Args Hash | Binding Test |
| Doppelter Effekt | stabiler Idempotency-Key | Retry/Concurrency Contracts |
| Audit-Manipulation | SHA-256 Kette | Audit Verify |
| Datenleck im Audit | Detail Hashes | No-Raw-Args Test |
| Flooding | Principal/Tool Quota | Quota Test |

Residual Risks: Produktions-Identity, durable/distributed State, Upstream-Provider-Korrektheit, Infrastruktur-Egress und Key Management.
