# Connector-Hub-Integration

Empfohlen wird ein separater Sidecar/Service. Connector Hub verwaltet Provider, Publikationen, Credentials und Ausführung; das MCP Gateway kontrolliert Sichtbarkeit und Autorität für AI Clients.

| MCP Tool | Publikation | Pflicht |
|---|---|---|
| `orders.lookup` | Order Read | tenant-aware |
| `inventory.check` | Inventory Read | tenant-aware |
| `customer.documents.list` | Permitted Documents | bereits authorization-filtered |
| `orders.cancel` | Cancellation | stabiler Idempotency-Key |

Verwende ein eigenes, minimal berechtigtes Connector-Hub-Service-Credential. Pfade und Ziele sind Deployment-Konfiguration, niemals Modellargumente. `integration/connector-hub/` enthält das Mapping-Beispiel.
