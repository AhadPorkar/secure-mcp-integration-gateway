# Governing MCP tools instead of giving AI raw integration authority

The fastest way to make an enterprise API available to an AI system is to expose a flexible tool. It is also an easy way to blur several security boundaries at once: identity, tenant selection, downstream credentials, network destination and business approval.

For this portfolio project I took the opposite approach. My existing Connector Hub remains the provider execution layer. A separate Java 21 gateway presents only four MCP-facing capabilities: three reads and one write. The model does not get a generic HTTP client, SQL executor, tenant selector or provider URL.

The write path is the most important part. Calling `orders.cancel` without an approval ID does not cancel anything. It creates a pending approval bound to the authenticated tenant and requester, the immutable tool revision and the SHA-256 fingerprint of canonical business arguments. A different reviewer in the same tenant must approve it. On execution the gateway reconstructs the fingerprint. A changed reason, order, tenant, requester or revision fails closed.

I also separated the caller credential from the Connector Hub credential. The MCP bearer token is validated for issuer, expiry and audience, but is never forwarded downstream. Connector Hub receives a dedicated, limited service credential and only fixed configured provider paths. Non-loopback destinations require HTTPS plus an explicit host allowlist.

The project targets a focused tool subset of the modern MCP `2026-07-28` era and includes protected-resource metadata, authorization-sensitive private caching for tool discovery, and method/name/protocol consistency checks. I deliberately do not call it full MCP conformance.

The release-candidate evidence is denial-heavy: wrong audience, expired/tampered tokens, cross-tenant reads, SQL/URL injection, tenant injection, self-approval, cross-tenant review, argument tampering, duplicate execution, audit integrity and quota abuse. The deterministic local gate currently contains 77 passed checks: 26 Java contracts, 40 live HTTP/MCP attack scenarios and 11 repository/release checks.

Native browser acceptance and the official MCP TypeScript v2 client are separate release gates because I prefer a visible unverified boundary to an inflated green number.

The design principle I wanted the repository to demonstrate is straightforward: an AI system can select among capabilities that policy already authorized. Identity, tenant, destination, approval and business authority remain deterministic system concerns.
