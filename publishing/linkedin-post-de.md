# LinkedIn-Post - Deutsch

Ich habe ein Security-first MCP Gateway für Enterprise-Integrationen gebaut.

Die interessante Frage war für mich nicht: „Wie mache ich eine API zu einem AI-Tool?“ Sondern: **Welche Autorität darf ein AI-Client tatsächlich erhalten?**

Mein Connector Hub bleibt die Provider-Ausführungsengine. Davor sitzt ein separater Java-21-Security-Adapter.

Vier Capabilities werden angeboten: Order Lookup, Inventory Check, erlaubte Kundendokumente und Order Cancellation. Die ersten drei sind Read-Operationen. Beim Cancel darf das Modell nur den Vorgang anstossen; ein separater menschlicher Reviewer muss die exakte Operation genehmigen, bevor der Write ausgeführt wird.

Die wichtigsten Controls sind im Code und in Angriffstests sichtbar:

- Tenant stammt aus verifizierter Identität, nie aus Modellargumenten;
- `tools/list` ist scope-gefiltert;
- Token Issuer, Ablauf und Audience werden geprüft;
- Caller-Bearer-Tokens werden nie an Connector Hub weitergereicht;
- der Upstream verwendet ein eigenes Least-Privilege-Service-Credential;
- beliebiges SQL und dynamische Ziel-URLs werden gar nicht angeboten;
- unbekannte Argumente wie `url`, `tenant` oder Credentials werden abgelehnt;
- Approval ist an Tenant, Requester, unveränderliche Tool-Revision und SHA-256-Fingerprint der kanonischen Argumente gebunden;
- Self-Approval und Cross-Tenant-Approval werden blockiert;
- die Approval-ID wird zum Upstream-Idempotency-Key;
- Quoten und eine manipulationsnachweisbare Audit-Hashkette werden erzwungen;
- die Reviewer-Konsole nutzt strikte CSP und speichert das Reviewer-Token nicht im Browser.

Auch der MCP-Wire-Contract ist Teil der Security-Grenze. Der RC zielt bewusst auf einen fokussierten Teil der modernen Revision `2026-07-28`, inklusive aktueller Discovery/List-Metadaten und Protected Resource Metadata.

Aktuelle deterministische Evidenz: **77 Checks bestanden** - 26 Java Security-/Contract-Assertions, 40 echte HTTP/MCP-Angriffsszenarien und 11 Repository-/Release-Integrity-Checks.

Native Browser Acceptance und den offiziellen MCP TypeScript v2 Client behandle ich als separate Release-Gates, statt einen Test als bestanden zu deklarieren, den die Authoring-Umgebung nicht ausführen konnte.

Der Design-Grundsatz ist einfach:

**AI darf zwischen autorisierten Capabilities wählen. Identität, Tenant, Zielsystem, Freigabe und Business-Autorität darf sie nicht selbst erzeugen.**

#Java #MCP #AIEngineering #CyberSecurity #EnterpriseArchitecture #ZeroTrust #BackendEngineering #SoftwareArchitecture
