# MCP-Tools kontrollieren statt AI rohe Integrationsautorität zu geben

Der schnellste Weg, eine Enterprise-API für AI verfügbar zu machen, ist ein flexibles Tool. Gleichzeitig verschwimmen dabei schnell mehrere Security-Grenzen: Identität, Tenant-Auswahl, Downstream-Credentials, Netzwerkziel und Business-Freigabe.

Für dieses Portfolio-Projekt habe ich bewusst den Gegenweg gewählt. Mein bestehender Connector Hub bleibt die Provider-Ausführungsschicht. Ein separater Java-21-Gateway stellt nur vier MCP-Fähigkeiten bereit: drei Reads und einen Write. Das Modell erhält weder generischen HTTP-Zugriff noch SQL-Executor, Tenant-Selector oder Provider-URL.

Der Write-Pfad ist der wichtigste Teil. Ein `orders.cancel` ohne Approval-ID storniert nichts, sondern erzeugt eine Pending Approval. Sie bindet den authentifizierten Tenant und Requester, die unveränderliche Tool-Revision und den SHA-256-Fingerprint der kanonischen Business-Argumente. Ein anderer Reviewer im selben Tenant muss genehmigen. Vor der Execution rekonstruiert das Gateway die Bindung. Eine geänderte Begründung, Order, Tenant, Requester oder Revision führt zu einem geschlossenen Deny.

Auch Credentials sind getrennt. Der MCP-Bearer-Token wird auf Issuer, Ablauf und Audience geprüft, aber nie an Connector Hub weitergereicht. Connector Hub erhält ein eigenes, eingeschränktes Service-Credential und nur feste konfigurierte Provider-Pfade. Nicht-Loopback-Ziele benötigen HTTPS und eine explizite Host-Allowlist.

Das Projekt zielt bewusst auf einen fokussierten Tool-Teil der modernen MCP-Revision `2026-07-28`, inklusive Protected Resource Metadata, authorization-sensitivem Private Caching bei Tool Discovery und Protocol/Method/Name-Konsistenzprüfungen. Ich bezeichne das nicht als vollständige MCP-Konformität.

Die RC-Evidenz ist bewusst denial-heavy: falsche Audience, abgelaufene/manipulierte Tokens, Cross-Tenant Reads, SQL-/URL-/Tenant-Injection, Self-Approval, Cross-Tenant Review, Argument-Tampering, Duplicate Execution, Audit-Integrität und Quota-Missbrauch. Der deterministische lokale Gate enthält aktuell 77 bestandene Checks: 26 Java Contracts, 40 echte HTTP/MCP-Angriffsszenarien und 11 Repository-/Release-Checks.

Native Browser Acceptance und der offizielle MCP TypeScript v2 Client bleiben separate Release-Gates. Eine sichtbare, noch nicht verifizierte Grenze ist mir lieber als eine künstlich aufgeblähte grüne Zahl.

Das zentrale Designprinzip: AI darf zwischen bereits durch Policy autorisierten Fähigkeiten wählen. Identität, Tenant, Ziel, Approval und Business-Autorität bleiben deterministische Systemaufgaben.
