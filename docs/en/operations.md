# Operations

## Local startup

`python scripts/run_demo.py` builds the Java sources, binds to loopback and writes short-lived demo tokens to `.local/access.json`.

## Production checklist

Terminate TLS at a controlled ingress; replace demo HMAC validation with OIDC/OAuth + trusted JWKS; use durable shared approval/audit/quota stores; rotate the Connector Hub service credential; restrict network egress; add metrics and trace correlation; validate clock synchronization; test backup/restore; and perform an independent security review.

## Logs and audit

The audit ledger stores hashes of details rather than raw business arguments. Production observability should correlate request IDs, approval IDs and upstream receipts without turning logs into a secondary data-exfiltration channel.

## Scaling

The RC uses in-memory state and is intentionally single-process. Do not place multiple replicas behind a load balancer until approval state, quota counters, audit ordering and idempotent execution are moved to shared durable infrastructure.
