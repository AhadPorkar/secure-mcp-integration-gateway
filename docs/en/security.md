# Security model

## Identity and audience

The runnable demo verifies HMAC-signed tokens so no external identity system is needed. It validates issuer, audience, expiry, subject and tenant. The audience check matters because a bearer token minted for another API must not become authority at this gateway.

Production must replace this seam with OAuth/OIDC access-token validation against trusted issuer metadata/JWKS and preserve an expected resource/audience check.

## Credential separation

The Connector Hub adapter has its own dedicated credential. Caller `Authorization` is never an input to the upstream client. This prevents indiscriminate token passthrough across resource-server boundaries.

## Tenant isolation

Tenant is identity-derived. None of the tool schemas accepts `tenant`. Every data call carries the verified tenant internally. Cross-tenant demo identifiers return generic `not_found` to reduce enumeration leakage.

## Tool and destination control

The catalog is fixed; schemas use `additionalProperties:false`; identifiers have bounded character sets; free text is length/control-character checked. There is no arbitrary SQL or destination tool. External Connector Hub hosts require HTTPS plus explicit allowlisting and fixed relative paths.

## Approval and replay

Write approval binds tenant, requester, tool revision and canonical argument hash. Self-approval is denied. Approval expiration is checked at execution. The approval ID is reused as the upstream idempotency key.

## Browser boundary

The reviewer console uses a strict CSP, dynamic DOM construction via `textContent`, and no local/session storage for reviewer credentials. It is a convenience console, not an identity provider.

## Audit

Audit entries are chained by SHA-256 and store detail hashes rather than raw business arguments. This demonstrates tamper evidence, not immutable compliance storage.
