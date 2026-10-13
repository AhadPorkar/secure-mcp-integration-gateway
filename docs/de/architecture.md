# Architektur

Das Gateway ist ein separater Security-Adapter zwischen MCP Host und Connector Hub. Connector Hub bleibt die Provider-Ausführungsengine; Credentials des Hubs werden nie an die Modellgrenze gegeben.

```text
MCP Host -> Secure MCP Gateway -> Connector Hub -> Enterprise Systeme
             | Identity/Tenant
             | Schema/Quota
             | Approval
             + Audit Hash Chain
```

## Request-Pfad

1. JSON-RPC und MCP-`2026-07-28` Metadaten werden validiert.
2. Geschützte Operationen prüfen Issuer, Audience, Ablauf, Subject und Tenant.
3. `tools/list` ist scope-gefiltert und verwendet `private` Cache Scope.
4. `tools/call` lehnt unbekannte Felder ab. Tenant und Destination sind keine Tool-Argumente.
5. Quoten werden vor Provider-Ausführung geprüft.
6. Read-Tools laufen direkt über den tenant-aware Connector-Hub-Client.
7. Ein Write ohne Approval-ID erzeugt nur eine Freigabe.
8. Ein exakt freigegebener Write nutzt einen stabilen Upstream-Idempotency-Key.
9. Security-Entscheidungen landen in einer SHA-256-verknuepften Audit-Kette.

Der RC haelt Approval, Quota und Audit bewusst im Prozess. Produktion benötigt durable Shared Stores und robuste Transaktions-/Retry-Semantik.
