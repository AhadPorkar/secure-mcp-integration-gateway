# Connector Hub integration handoff

This gateway is intentionally deployable **without changing Connector Hub source code**.

## Handoff sequence

1. Publish four immutable Connector Hub provider revisions for order lookup, inventory lookup, permitted-document listing and cancellation.
2. Create a dedicated least-privilege service credential limited to those publications.
3. Copy `.env.example` into your private deployment configuration and never commit the real values.
4. Configure the exact published paths and HTTPS Connector Hub base URL.
5. Add the production hostname to `CONNECTOR_HUB_ALLOWED_HOSTS`.
6. Start with the three read capabilities and prove tenant isolation.
7. Verify the cancellation provider honors `Idempotency-Key` under retry/concurrency before enabling the write capability.
8. Run the attack matrix again with your real adapter contract test fixture.

`provider-mapping.example.json` documents the intended logical contract. Paths are fixed deployment configuration; a model never chooses a provider URL, host, SQL statement or publication identifier.

## Generic HTTP envelope

The reference adapter sends:

```json
{
  "tenant": "<verified-tenant>",
  "input": { "...": "..." }
}
```

It expects a JSON object response and supplies a stable `Idempotency-Key` for the write operation. If your selected Connector Hub publication uses a different envelope or credential header, adapt **only `HttpConnectorHubClient` and its contract tests**. Do not move destination choice, tenancy or credential handling into MCP arguments.

## Explicit integration gate

The public Connector Hub repository provides the architectural foundation for published/scoped providers, but this RC does **not** claim that the generic envelope above was executed against your deployed Connector Hub. Treat that as a real-environment acceptance gate and document the exact provider revisions used when you complete it.
