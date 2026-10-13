# Connector Hub integration

The recommended deployment is a separate sidecar/service. Connector Hub remains responsible for provider publication, provider credentials and execution. The MCP gateway decides what an AI-facing client is allowed to see and invoke.

## Logical mapping

| MCP tool | Connector Hub publication | Required provider behavior |
|---|---|---|
| `orders.lookup` | immutable order-read publication | tenant-aware read |
| `inventory.check` | immutable stock-read publication | tenant-aware read |
| `customer.documents.list` | permitted-document publication | authorization-filtered results |
| `orders.cancel` | immutable cancellation publication | accepts stable idempotency key |

The paths are deployment configuration, not model input. Do not expose a generic provider ID, SQL statement, URL, host or path field in an MCP schema.

## Credential model

Create one Connector Hub service credential dedicated to this gateway and scope it only to the four selected publications. The HTTP adapter does not accept a caller bearer token and therefore cannot accidentally forward an access token minted for a different audience.

## Existing Connector Hub relationship

This project is additive. It does not require changing the dependency-light Java 21 core of the existing Connector Hub repository. `integration/connector-hub/` contains an example mapping contract and deployment notes.

## Production acceptance

Before enabling the real write provider, prove: tenant enforcement inside the provider boundary, idempotency under concurrent retries, timeout behavior, credential rotation, audit correlation, and a deny-by-default egress policy.
