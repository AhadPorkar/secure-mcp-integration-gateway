# Secure MCP Integration Gateway - Release Brief

## Positioning

A Java 21 security adapter that exposes a deliberately small set of Connector Hub capabilities through a governed MCP boundary without turning the model into an authorization authority.

## What the release demonstrates

Identity-aware API design, tenant isolation, audience validation, least-privilege tool discovery, closed argument contracts, service-to-service credential separation, human approval for consequential writes, exact approval binding, idempotent retry semantics, upstream SSRF controls, quotas, tamper-evident audit and adversarial testing.

## Verified release-candidate evidence

- **26** Java security/contract assertions passed.
- **40** live HTTP/MCP attack scenarios passed.
- **11** repository/release-integrity checks passed.
- **77 deterministic checks total**, with Java compiled using `-Xlint:all -Werror`.

## Modern MCP scope

The gateway implements a focused `2026-07-28` tool subset: `server/discover`, `tools/list`, `tools/call`, request metadata/header binding, current result/cache fields and protected-resource metadata. It does not claim full MCP conformance.

## Deliberate boundaries

The built-in HMAC identity seam and in-memory approval/audit/quota state exist for reproducibility. Production OIDC/JWKS, durable distributed state, real Connector Hub acceptance, native-browser acceptance and official MCP-v2-SDK interoperability remain explicit release gates.
