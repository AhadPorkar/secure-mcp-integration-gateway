# Secure MCP Integration Gateway - Release Brief

## Positionierung

Ein Java-21-Security-Adapter, der einen bewusst kleinen Teil von Connector-Hub-Fähigkeiten über eine kontrollierte MCP-Grenze bereitstellt, ohne das Modell zur Autorisierungsinstanz zu machen.

## Engineering-Signal

Identity-aware API Design, Mandantentrennung, Audience Validation, Least-Privilege Tool Discovery, geschlossene Argumentvertraege, Service-to-Service Credential Separation, Human Approval für konsequente Writes, exaktes Approval Binding, Idempotency, Upstream-SSRF-Kontrollen, Quoten, Tamper-evident Audit und adversariales Testing.

## Verifizierte RC-Evidenz

- **26** Java Security-/Contract-Assertions bestanden.
- **40** echte HTTP/MCP-Angriffsszenarien bestanden.
- **11** Repository-/Release-Integrity-Checks bestanden.
- **77 deterministische Checks insgesamt**, Java kompiliert mit `-Xlint:all -Werror`.

## Moderner MCP-Scope

Implementiert ist ein fokussierter `2026-07-28` Tool-Teilumfang: `server/discover`, `tools/list`, `tools/call`, Request-Metadaten/Header-Bindung, aktuelle Result/Cache-Felder und Protected Resource Metadata. Keine Behauptung vollständiger MCP-Konformität.

## Bewusste Grenzen

HMAC-Demo-Identity und In-Memory Approval/Audit/Quota dienen der Reproduzierbarkeit. Production OIDC/JWKS, durable Distributed State, echte Connector-Hub-Akzeptanz, Native Browser Acceptance und offizieller MCP-v2-SDK-Test bleiben explizite Release-Gates.
