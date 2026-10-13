# Security policy and project boundary

## Supported status

`1.0.0-rc.1` is a portfolio/reference release candidate, not a production-certified security product.

## Security invariants in the RC

- token issuer, audience and expiration are validated;
- tenant is identity-derived and is not a tool argument;
- tool discovery and invocation are scope-controlled;
- unknown tool arguments fail closed;
- caller bearer tokens are never forwarded to Connector Hub;
- non-loopback upstream hosts require HTTPS plus explicit allowlisting;
- write approval binds requester, tenant, tool revision and canonical argument fingerprint;
- self-approval and cross-tenant review are denied;
- audit entries form a SHA-256 chain and do not retain raw business arguments;
- reviewer credentials are not stored in browser local/session storage.

## Not production-ready by itself

The local HMAC identity seam, in-memory approval/audit/quota state and synthetic provider backend are demo mechanisms. Production requires OAuth/OIDC with trusted JWKS, TLS, durable shared state, key rotation, restricted egress, monitoring, backup/restore and independent security assessment.

## Reporting

Do not put credentials, customer data or exploit details for a real system into a public issue. For a real deployment, use the organization's private security-reporting channel.
