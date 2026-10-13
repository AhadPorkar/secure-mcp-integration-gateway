# Release checklist

- [ ] `python scripts/test.py` passes on the exact release commit.
- [ ] `docs/evidence/test-run.txt` was regenerated from that commit.
- [ ] `.local/`, secrets, customer data, private keys and service credentials are not tracked.
- [ ] README does not claim full MCP conformance.
- [ ] Fixed Connector Hub paths and host allowlist were reviewed.
- [ ] Real write provider proves idempotency under retries/concurrency.
- [ ] Production tokens use OIDC/OAuth + trusted JWKS rather than the demo HMAC seam.
- [ ] Approval, audit and quota state are durable before horizontal scaling.
- [ ] Native browser acceptance was run in an unrestricted environment if the reviewer UI is part of the release claim.
- [ ] Official MCP v2 client acceptance was run before advertising broad SDK interoperability.
- [ ] Threat review covers cross-tenant access, SSRF, token forwarding, replay, approval bypass and log leakage.
