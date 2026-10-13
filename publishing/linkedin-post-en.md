# LinkedIn post - English

I built a security-first MCP gateway for enterprise integrations.

The interesting question was not, “How do I expose an API as an AI tool?” It was: **what authority should an AI client actually receive?**

I kept my Connector Hub as the provider execution engine and placed a separate Java 21 security adapter in front of it.

The gateway makes four capabilities available: order lookup, inventory lookup, permitted-document listing, and order cancellation. The first three are reads. Cancellation is deliberately different: the model can propose it, but a separate human reviewer must approve the exact operation before the write can execute.

The controls I wanted to make visible in code rather than presentation slides:

- tenant comes from verified identity, never from model arguments;
- `tools/list` is scope-filtered;
- access-token issuer, expiration and audience are checked;
- caller bearer tokens are never forwarded to Connector Hub;
- the upstream uses a separate least-privilege service credential;
- arbitrary SQL and dynamic destination URLs are not exposed;
- unknown arguments such as `url`, `tenant` or credentials are rejected;
- write approval is bound to tenant, requester, immutable tool revision and the SHA-256 fingerprint of canonical arguments;
- self-approval and cross-tenant approval are blocked;
- the approval ID becomes the upstream idempotency key;
- quotas and a tamper-evident audit hash chain are enforced;
- the reviewer console uses a strict CSP and never persists the reviewer token in browser storage.

I also treated the MCP wire contract as part of the security boundary. The release targets a focused subset of the modern `2026-07-28` era, including current discovery/list result metadata and protected-resource metadata.

Current deterministic evidence: **77 checks passed** - 26 Java security/contract assertions, 40 live HTTP/MCP attack scenarios, and 11 repository/release-integrity checks.

I keep native-browser acceptance and the official MCP TypeScript v2 client as separate release gates rather than claiming they ran where the environment did not allow them.

The design principle is simple:

**AI may choose among authorized capabilities. It should not be able to manufacture identity, tenant, destination, approval, or business authority.**

#Java #MCP #AIEngineering #CyberSecurity #EnterpriseArchitecture #ZeroTrust #BackendEngineering #SoftwareArchitecture
