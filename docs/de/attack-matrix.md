# Ausführbare Angriffsmatrix

`tests/attack_matrix.py` startet den echten Java-Prozess und prüft **40 Live HTTP/MCP Security-Szenarien**. Dazu kommen **26 Java Contract Assertions** und **11 Repository-Integrity-Checks**.

Geprüft werden unter anderem falsche Audience, abgelaufene/manipulierte Tokens, Least Privilege, Cross-Tenant-Zugriffe, MCP Header/Body-Mismatch, beliebiges SQL, URL-/Tenant-Injection, Self-Approval, Cross-Tenant-Approval, Argument-Tampering, idempotente Writes, Audit-Kette und Quoten.

Die exakte Ausgabe steht unter `docs/evidence/test-run.txt`. Native Browser Acceptance und offizieller MCP-v2-SDK-Test sind separate manuelle Release-Gates und nicht Teil der 77 lokalen Passes.
