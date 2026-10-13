# Fuenf-Minuten-Recruiter-Demo

1. Scope-gefilterten Tool-Katalog zeigen.
2. `ORD-A100` als ACME lesen.
3. Fremden `ORD-B200` versuchen: `not_found`.
4. `sql.execute`, `url` und Tenant-Injection zeigen: alles Deny.
5. `orders.cancel`: nur Approval, kein Write.
6. Mit separatem Reviewer exakt genehmigen.
7. Begründung manipulieren: Approval Binding blockiert.
8. Exakten Call ausführen und idempotent wiederholen.
9. Audit-Hash-Kette und 77-Check-Evidenz zeigen.

Botschaft: **AI darf eine erlaubte Capability wählen, aber keine Identität, keinen Tenant, kein Ziel und keine Business-Autorität erfinden.**
